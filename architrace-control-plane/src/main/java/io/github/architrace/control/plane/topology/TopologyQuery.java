/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TopologyQuery {

  private static final Comparator<Scope> SCOPE_ORDER =
      Comparator.comparing(Scope::project)
          .thenComparing(Scope::environment)
          .thenComparing(Scope::cluster);

  private static final Comparator<Agent> AGENT_ORDER =
      Comparator.comparing(Agent::scope, SCOPE_ORDER).thenComparing(Agent::name);

  private final AgentStore agents;
  private final SnapshotStore snapshots;
  private final AgentLiveness liveness;
  private final TopologyMetrics metrics;
  private final Clock clock;

  public TopologyQuery(
      AgentStore agents,
      SnapshotStore snapshots,
      AgentLiveness liveness,
      TopologyMetrics metrics,
      Clock clock) {
    this.agents = agents;
    this.snapshots = snapshots;
    this.liveness = liveness;
    this.metrics = metrics;
    this.clock = clock;
  }

  public TopologyGraph currentGraph(Scope scope, Instant at) {
    requireKnown(scope);
    return metrics.recordGraphQuery(
        () -> GraphMerger.merge(scope, at, snapshots.latestPerAgent(scope, at)));
  }

  public List<NodeView> services(Scope scope, Instant at) {
    return NodeViews.of(currentGraph(scope, at), NodeType.SERVICE);
  }

  public Page<SnapshotSummary> snapshots(SnapshotFilter filter, PageRequest page) {
    requireKnown(filter.scope());
    return snapshots.list(filter, page);
  }

  public Snapshot snapshot(SnapshotId id) {
    return snapshots.find(id).orElseThrow(() -> new SnapshotNotFoundException(id));
  }

  public List<ScopeSummary> scopes() {
    Instant now = clock.instant();
    Map<Scope, List<Agent>> byScope =
        agents.all().stream()
            .collect(
                Collectors.groupingBy(
                    Agent::scope, () -> new TreeMap<>(SCOPE_ORDER), Collectors.toList()));
    return byScope.entrySet().stream()
        .map(entry -> summarise(entry.getKey(), entry.getValue(), now))
        .toList();
  }

  public List<AgentStatus> agents() {
    Instant now = clock.instant();
    return agents.all().stream()
        .sorted(AGENT_ORDER)
        .map(agent -> new AgentStatus(agent, liveness.isLive(agent, now)))
        .toList();
  }

  private void requireKnown(Scope scope) {
    if (agents.all().stream().noneMatch(agent -> agent.scope().equals(scope))) {
      throw new ScopeNotFoundException(scope);
    }
  }

  private ScopeSummary summarise(Scope scope, List<Agent> scopeAgents, Instant now) {
    List<Snapshot> latest = snapshots.latestPerAgent(scope, now);
    TopologyGraph graph = GraphMerger.merge(scope, now, latest);
    int live = (int) scopeAgents.stream().filter(agent -> liveness.isLive(agent, now)).count();
    Set<String> namespaces =
        graph.nodes().stream()
            .flatMap(node -> node.attributes().deployments().stream())
            .map(Deployment::namespace)
            .filter(namespace -> !namespace.isEmpty())
            .collect(Collectors.toSet());
    return new ScopeSummary(
        scope,
        scopeAgents.size(),
        live,
        count(graph, NodeType.SERVICE),
        count(graph, NodeType.TOPIC),
        namespaces.size(),
        latest.stream().map(snapshot -> snapshot.window().end()).max(Comparator.naturalOrder()));
  }

  private static int count(TopologyGraph graph, NodeType type) {
    return (int) graph.nodes().stream().filter(node -> node.type() == type).count();
  }
}
