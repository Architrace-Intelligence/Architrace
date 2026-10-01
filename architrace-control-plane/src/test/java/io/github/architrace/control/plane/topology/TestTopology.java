/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class TestTopology {

  public static final Scope SCOPE = new Scope("webshop", "PROD", "k8s-prod-eu1");
  public static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");

  private TestTopology() {}

  public static TopologyNode service(String name, String version, String namespace) {
    return new TopologyNode(
        "service:" + name,
        NodeType.SERVICE,
        name,
        new NodeAttributes(
            Set.of(version), Set.of(new Deployment(SCOPE.cluster(), namespace)), Map.of()));
  }

  public static TopologyNode database(String name) {
    return new TopologyNode("db:postgresql/" + name, NodeType.DATABASE, name, NodeAttributes.none());
  }

  public static TopologyNode topic(String name) {
    return new TopologyNode("topic:kafka/" + name, NodeType.TOPIC, name, NodeAttributes.none());
  }

  public static TopologyEdge edge(
      TopologyNode source, TopologyNode target, EdgeKind kind, long calls) {
    return new TopologyEdge(source.id(), target.id(), kind, new EdgeMetrics(calls, 0, 5, 10, 20, 50));
  }

  public static Snapshot snapshot(
      AgentId agent, Instant windowEnd, List<TopologyNode> nodes, List<TopologyEdge> edges) {
    return snapshot(agent, SCOPE, windowEnd, nodes, edges);
  }

  public static Snapshot snapshot(
      AgentId agent,
      Scope scope,
      Instant windowEnd,
      List<TopologyNode> nodes,
      List<TopologyEdge> edges) {
    return new Snapshot(
        agent,
        scope,
        new TimeWindow(windowEnd.minusSeconds(60), windowEnd),
        windowEnd.plusSeconds(1),
        nodes,
        edges);
  }
}
