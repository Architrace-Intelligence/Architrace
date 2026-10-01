/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.control.plane.topology.Agent;
import io.github.architrace.control.plane.topology.AgentRegistration;
import io.github.architrace.control.plane.topology.Deployment;
import io.github.architrace.control.plane.topology.EdgeKind;
import io.github.architrace.control.plane.topology.EdgeMetrics;
import io.github.architrace.control.plane.topology.NodeAttributes;
import io.github.architrace.control.plane.topology.NodeType;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.Snapshot;
import io.github.architrace.control.plane.topology.SnapshotId;
import io.github.architrace.control.plane.topology.TimeWindow;
import io.github.architrace.control.plane.topology.TopologyEdge;
import io.github.architrace.control.plane.topology.TopologyNode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

class JdbcSnapshotStoreTest extends JdbcStoreTest {

  private static final Scope SCOPE = new Scope("webshop", "PROD", "k8s-prod-eu1");
  private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");

  @Autowired JdbcSnapshotStore snapshots;
  @Autowired JdbcAgentStore agents;
  @Autowired JdbcClient jdbc;

  @Test
  void savesAndReadsBackASnapshotWithNodesAndEdges() {
    Agent agent = agents.register(new AgentRegistration("prod-eu1-a", "0.4.0", SCOPE), NOW);
    NodeAttributes serviceAttributes =
        new NodeAttributes(
            Set.of("2.8.1", "2.8.0"),
            Set.of(new Deployment("k8s-prod-eu1", "orders")),
            Map.of("replicas", "3"));
    Snapshot snapshot =
        new Snapshot(
            agent.id(),
            SCOPE,
            new TimeWindow(NOW.minusSeconds(60), NOW),
            NOW.plusSeconds(1),
            List.of(
                new TopologyNode(
                    "service:orders/orders-service", NodeType.SERVICE, "orders-service", serviceAttributes),
                new TopologyNode(
                    "db:postgresql/orders", NodeType.DATABASE, "orders", NodeAttributes.none()),
                new TopologyNode(
                    "topic:kafka/order-events", NodeType.TOPIC, "order-events", NodeAttributes.none())),
            List.of(
                new TopologyEdge(
                    "service:orders/orders-service",
                    "db:postgresql/orders",
                    EdgeKind.SYNC,
                    new EdgeMetrics(24000, 3, 3, 9, 22, 140)),
                new TopologyEdge(
                    "service:orders/orders-service",
                    "topic:kafka/order-events",
                    EdgeKind.PUBLISH,
                    new EdgeMetrics(8700, 0, 2, 5, 9, 60))));

    SnapshotId id = snapshots.save(snapshot);

    Snapshot found = snapshots.find(id).orElseThrow();
    assertThat(found)
        .usingRecursiveComparison()
        .ignoringFields("nodes", "edges")
        .isEqualTo(snapshot);
    assertThat(found.nodes()).containsExactlyInAnyOrderElementsOf(snapshot.nodes());
    assertThat(found.edges()).containsExactlyInAnyOrderElementsOf(snapshot.edges());
    Integer nodeCount =
        jdbc.sql("select node_count from snapshot where id = :id")
            .param("id", id.value())
            .query(Integer.class)
            .single();
    assertThat(nodeCount).isEqualTo(3);
    String versions =
        jdbc.sql("select attributes ->> 'versions' from snapshot_node where node_id = :node")
            .param("node", "service:orders/orders-service")
            .query(String.class)
            .single();
    assertThat(versions).contains("2.8.1").contains("2.8.0");
  }

  @Test
  void keepsSnapshotsOfTheSameAgentApart() {
    Agent agent = agents.register(new AgentRegistration("prod-eu1-b", "0.4.0", SCOPE), NOW);
    Snapshot first = emptySnapshot(agent, NOW.minusSeconds(120), NOW.minusSeconds(60));
    Snapshot second = emptySnapshot(agent, NOW.minusSeconds(60), NOW);

    SnapshotId firstId = snapshots.save(first);
    SnapshotId secondId = snapshots.save(second);

    assertThat(firstId).isNotEqualTo(secondId);
    assertThat(snapshots.find(firstId)).contains(first);
    assertThat(snapshots.find(secondId)).contains(second);
    assertThat(snapshots.find(new SnapshotId(Long.MAX_VALUE))).isEmpty();
  }

  @Test
  void latestPerAgentPicksTheNewestWindowOfEachAgentAtOrBeforeTheInstant() {
    Agent a = agents.register(new AgentRegistration("latest-a", "0.4.0", SCOPE), NOW);
    Agent b = agents.register(new AgentRegistration("latest-b", "0.4.0", SCOPE), NOW);
    Scope dev = new Scope("webshop", "DEV", "k8s-dev");
    Agent elsewhere = agents.register(new AgentRegistration("latest-c", "0.4.0", dev), NOW);
    Instant at = NOW.minusSeconds(30);
    snapshots.save(emptySnapshot(a, at.minusSeconds(180), at.minusSeconds(120)));
    Snapshot latestA = emptySnapshot(a, at.minusSeconds(120), at.minusSeconds(60));
    snapshots.save(latestA);
    Snapshot latestB = emptySnapshot(b, at.minusSeconds(60), at);
    snapshots.save(latestB);
    snapshots.save(emptySnapshot(b, at, at.plusSeconds(60)));
    snapshots.save(
        new Snapshot(
            elsewhere.id(),
            dev,
            new TimeWindow(at.minusSeconds(60), at),
            at,
            List.of(),
            List.of()));

    List<Snapshot> latest = snapshots.latestPerAgent(SCOPE, at);

    assertThat(latest).containsExactly(latestA, latestB);
  }

  @Test
  void deleteOlderThanRemovesExpiredSnapshotsWithTheirRowsInBatches() {
    Agent agent = agents.register(new AgentRegistration("retention", "0.4.0", SCOPE), NOW);
    Instant cutoff = NOW.minusSeconds(3600);
    SnapshotId oldest =
        snapshots.save(snapshotWithNode(agent, cutoff.minusSeconds(120), "service:old-1"));
    SnapshotId older =
        snapshots.save(snapshotWithNode(agent, cutoff.minusSeconds(60), "service:old-2"));
    SnapshotId boundary = snapshots.save(emptySnapshot(agent, cutoff.minusSeconds(60), cutoff));
    SnapshotId recent = snapshots.save(emptySnapshot(agent, NOW.minusSeconds(60), NOW));

    int firstBatch = snapshots.deleteOlderThan(cutoff, 1);
    int secondBatch = snapshots.deleteOlderThan(cutoff, 1);
    int nothingLeft = snapshots.deleteOlderThan(cutoff, 1);

    assertThat(firstBatch).isEqualTo(1);
    assertThat(secondBatch).isEqualTo(1);
    assertThat(nothingLeft).isZero();
    assertThat(snapshots.find(oldest)).isEmpty();
    assertThat(snapshots.find(older)).isEmpty();
    assertThat(snapshots.find(boundary)).isPresent();
    assertThat(snapshots.find(recent)).isPresent();
    Integer orphanNodes =
        jdbc.sql("select count(*) from snapshot_node where node_id like 'service:old-%'")
            .query(Integer.class)
            .single();
    assertThat(orphanNodes).isZero();
  }

  private static Snapshot snapshotWithNode(Agent agent, Instant end, String nodeId) {
    return new Snapshot(
        agent.id(),
        SCOPE,
        new TimeWindow(end.minusSeconds(60), end),
        end,
        List.of(new TopologyNode(nodeId, NodeType.SERVICE, "old", NodeAttributes.none())),
        List.of());
  }

  private static Snapshot emptySnapshot(Agent agent, Instant start, Instant end) {
    return new Snapshot(agent.id(), SCOPE, new TimeWindow(start, end), end, List.of(), List.of());
  }
}
