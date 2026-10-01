/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology;

import static io.github.architrace.control.plane.topology.TestTopology.NOW;
import static io.github.architrace.control.plane.topology.TestTopology.SCOPE;
import static io.github.architrace.control.plane.topology.TestTopology.database;
import static io.github.architrace.control.plane.topology.TestTopology.edge;
import static io.github.architrace.control.plane.topology.TestTopology.service;
import static io.github.architrace.control.plane.topology.TestTopology.snapshot;
import static io.github.architrace.control.plane.topology.TestTopology.topic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TopologyQueryTest {

  private static final Scope DEV = new Scope("webshop", "DEV", "k8s-dev");
  private static final Scope ACCOUNTING = new Scope("accounting", "PROD", "k8s-prod-eu1");

  private final InMemoryAgentStore agents = new InMemoryAgentStore();
  private final InMemorySnapshotStore snapshots = new InMemorySnapshotStore();
  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final TopologyQuery query =
      new TopologyQuery(
          agents,
          snapshots,
          new AgentLiveness(Duration.ofSeconds(30)),
          new TopologyMetrics(registry),
          Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void currentGraphMergesTheLatestSnapshotOfEachAgentAtTheInstant() {
    Agent a = register("a", SCOPE, NOW);
    Agent b = register("b", SCOPE, NOW);
    Agent elsewhere = register("c", DEV, NOW);
    Instant at = NOW.minusSeconds(30);
    TopologyNode ordersA = service("orders", "2.8.1", "orders");
    TopologyNode ordersB = service("orders", "2.8.0", "orders");
    TopologyNode db = database("orders");
    TopologyNode events = topic("order-events");
    snapshots.save(
        snapshot(a.id(), at.minusSeconds(120), List.of(service("stale", "1.0", "ns")), List.of()));
    snapshots.save(
        snapshot(
            a.id(),
            at.minusSeconds(60),
            List.of(ordersA, db),
            List.of(edge(ordersA, db, EdgeKind.SYNC, 100))));
    snapshots.save(
        snapshot(
            b.id(),
            at,
            List.of(ordersB, events),
            List.of(edge(ordersB, events, EdgeKind.PUBLISH, 10))));
    snapshots.save(
        snapshot(b.id(), at.plusSeconds(60), List.of(service("future", "1.0", "ns")), List.of()));
    snapshots.save(
        snapshot(elsewhere.id(), DEV, at, List.of(service("dev-only", "1.0", "ns")), List.of()));

    TopologyGraph graph = query.currentGraph(SCOPE, at);

    assertThat(graph.at()).isEqualTo(at);
    assertThat(graph.nodes())
        .extracting(TopologyNode::id)
        .containsExactly("db:postgresql/orders", "service:orders", "topic:kafka/order-events");
    assertThat(graph.nodes().get(1).attributes().versions())
        .containsExactlyInAnyOrder("2.8.0", "2.8.1");
    assertThat(graph.edges()).hasSize(2);
    assertThat(registry.get("architrace.topology.query").timer().count()).isEqualTo(1);
  }

  @Test
  void currentGraphOfAScopeWithoutAgentsIsNotFound() {
    register("a", SCOPE, NOW);

    assertThatExceptionOfType(ScopeNotFoundException.class)
        .isThrownBy(() -> query.currentGraph(DEV, NOW))
        .withMessageContaining("webshop/DEV/k8s-dev");
  }

  @Test
  void servicesViewTheCurrentGraphThroughItsServiceNodes() {
    Agent a = register("a", SCOPE, NOW);
    TopologyNode orders = service("orders", "2.8.1", "orders");
    TopologyNode db = database("orders");
    snapshots.save(
        snapshot(
            a.id(),
            NOW.minusSeconds(60),
            List.of(orders, db),
            List.of(edge(orders, db, EdgeKind.SYNC, 100))));

    List<NodeView> services = query.services(SCOPE, NOW);

    assertThat(services)
        .singleElement()
        .satisfies(
            view -> {
              assertThat(view.node()).isEqualTo(orders);
              assertThat(view.inbound()).isEmpty();
              assertThat(view.outbound()).extracting(Dependency::node).containsExactly(db);
            });
    assertThat(registry.get("architrace.topology.query").timer().count()).isEqualTo(1);
  }

  @Test
  void snapshotsPageTheHistoryOfAKnownScopeNewestFirst() {
    Agent a = register("a", SCOPE, NOW);
    Agent b = register("b", SCOPE, NOW);
    Agent elsewhere = register("c", DEV, NOW);
    SnapshotId oldest =
        snapshots.save(
            snapshot(
                a.id(),
                NOW.minusSeconds(180),
                List.of(service("orders", "1.0", "orders")),
                List.of()));
    SnapshotId middle =
        snapshots.save(snapshot(b.id(), NOW.minusSeconds(120), List.of(), List.of()));
    SnapshotId newest =
        snapshots.save(snapshot(a.id(), NOW.minusSeconds(60), List.of(), List.of()));
    snapshots.save(snapshot(elsewhere.id(), DEV, NOW.minusSeconds(60), List.of(), List.of()));

    Page<SnapshotSummary> firstPage =
        query.snapshots(SnapshotFilter.all(SCOPE), new PageRequest(0, 2));
    Page<SnapshotSummary> secondPage =
        query.snapshots(SnapshotFilter.all(SCOPE), new PageRequest(1, 2));
    Page<SnapshotSummary> bounded =
        query.snapshots(
            new SnapshotFilter(
                SCOPE, Optional.of(NOW.minusSeconds(150)), Optional.of(NOW.minusSeconds(90))),
            new PageRequest(0, 50));

    assertThat(firstPage.items()).extracting(SnapshotSummary::id).containsExactly(newest, middle);
    assertThat(firstPage.totalItems()).isEqualTo(3);
    assertThat(firstPage.totalPages()).isEqualTo(2);
    assertThat(secondPage.items()).extracting(SnapshotSummary::id).containsExactly(oldest);
    assertThat(secondPage.items().get(0).nodeCount()).isEqualTo(1);
    assertThat(bounded.items()).extracting(SnapshotSummary::id).containsExactly(middle);
    assertThatExceptionOfType(ScopeNotFoundException.class)
        .isThrownBy(() -> query.snapshots(SnapshotFilter.all(ACCOUNTING), new PageRequest(0, 10)));
  }

  @Test
  void snapshotIsFoundByIdOrReportedMissing() {
    Agent a = register("a", SCOPE, NOW);
    Snapshot stored = snapshot(a.id(), NOW, List.of(service("orders", "1.0", "orders")), List.of());
    SnapshotId id = snapshots.save(stored);

    assertThat(query.snapshot(id)).isEqualTo(stored);
    assertThatExceptionOfType(SnapshotNotFoundException.class)
        .isThrownBy(() -> query.snapshot(new SnapshotId(99)))
        .withMessageContaining("99");
  }

  @Test
  void scopesSummariseAgentsLivenessAndTheCurrentGraph() {
    Agent a = register("a", SCOPE, NOW);
    Agent b = register("b", SCOPE, NOW.minus(Duration.ofMinutes(5)));
    register("c", DEV, NOW);
    register("d", ACCOUNTING, NOW);
    snapshots.save(
        snapshot(
            a.id(),
            NOW.minusSeconds(60),
            List.of(
                service("orders", "2.8.1", "orders"), database("orders"), topic("order-events")),
            List.of()));
    snapshots.save(
        snapshot(
            b.id(),
            NOW.minusSeconds(30),
            List.of(
                service("orders", "2.8.1", "orders-canary"),
                service("billing", "1.0", "billing"),
                topic("invoices")),
            List.of()));

    List<ScopeSummary> scopes = query.scopes();

    assertThat(scopes).extracting(ScopeSummary::scope).containsExactly(ACCOUNTING, DEV, SCOPE);
    assertThat(scopes.get(2))
        .isEqualTo(new ScopeSummary(SCOPE, 2, 1, 2, 2, 3, Optional.of(NOW.minusSeconds(30))));
    assertThat(scopes.get(1)).isEqualTo(new ScopeSummary(DEV, 1, 1, 0, 0, 0, Optional.empty()));
  }

  @Test
  void agentsAreOrderedByScopeAndNameWithTheirLiveness() {
    Agent silent = register("b", SCOPE, NOW.minus(Duration.ofMinutes(5)));
    Agent fresh = register("a", SCOPE, NOW);
    Agent dev = register("c", DEV, NOW);

    List<AgentStatus> statuses = query.agents();

    assertThat(statuses)
        .containsExactly(
            new AgentStatus(dev, true),
            new AgentStatus(fresh, true),
            new AgentStatus(silent, false));
  }

  private Agent register(String name, Scope scope, Instant lastSeen) {
    return agents.register(new AgentRegistration(name, "0.4.0", scope), lastSeen);
  }
}
