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

  private static Snapshot emptySnapshot(Agent agent, Instant start, Instant end) {
    return new Snapshot(agent.id(), SCOPE, new TimeWindow(start, end), end, List.of(), List.of());
  }
}
