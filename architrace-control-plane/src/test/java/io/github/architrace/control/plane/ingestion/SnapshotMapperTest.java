/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.ingestion;

import static io.github.architrace.control.plane.ingestion.SnapshotProtos.WINDOW_END;
import static io.github.architrace.control.plane.ingestion.SnapshotProtos.edge;
import static io.github.architrace.control.plane.ingestion.SnapshotProtos.node;
import static io.github.architrace.control.plane.ingestion.SnapshotProtos.ordersSnapshot;
import static io.github.architrace.control.plane.ingestion.SnapshotProtos.window;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.architrace.control.plane.topology.Agent;
import io.github.architrace.control.plane.topology.AgentId;
import io.github.architrace.control.plane.topology.Deployment;
import io.github.architrace.control.plane.topology.EdgeKind;
import io.github.architrace.control.plane.topology.NodeType;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.Snapshot;
import io.github.architrace.control.plane.topology.TopologyEdge;
import io.github.architrace.control.plane.topology.TopologyNode;
import io.github.architrace.grpc.proto.GraphSnapshot;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SnapshotMapperTest {

  private static final Scope SCOPE = new Scope("webshop", "PROD", "k8s-prod-eu1");
  private static final Agent AGENT =
      new Agent(new AgentId(7), "prod-eu1-a", "0.4.0", SCOPE, WINDOW_END, WINDOW_END);
  private static final Instant RECEIVED = WINDOW_END.plusSeconds(2);

  private final SnapshotMapper mapper = new SnapshotMapper();

  @Test
  void mapsNodesEdgesAndAttributes() {
    Snapshot snapshot = mapper.toSnapshot(AGENT, ordersSnapshot(), RECEIVED);

    assertThat(snapshot.agentId()).isEqualTo(AGENT.id());
    assertThat(snapshot.scope()).isEqualTo(SCOPE);
    assertThat(snapshot.window().end()).isEqualTo(WINDOW_END);
    assertThat(snapshot.receivedAt()).isEqualTo(RECEIVED);
    assertThat(snapshot.nodes())
        .extracting(TopologyNode::id, TopologyNode::type)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("service:orders/orders-service", NodeType.SERVICE),
            org.assertj.core.groups.Tuple.tuple("db:postgresql/orders", NodeType.DATABASE),
            org.assertj.core.groups.Tuple.tuple("topic:kafka/order-events", NodeType.TOPIC));
    TopologyNode service = snapshot.nodes().getFirst();
    assertThat(service.attributes().versions()).containsExactly("2.8.1");
    assertThat(service.attributes().deployments())
        .containsExactly(new Deployment("k8s-prod-eu1", "orders"));
    assertThat(service.attributes().labels()).containsEntry("replicas", "3");
    assertThat(snapshot.edges())
        .extracting(TopologyEdge::kind)
        .containsExactly(EdgeKind.SYNC, EdgeKind.PUBLISH);
    assertThat(snapshot.edges().getFirst().metrics().calls()).isEqualTo(24000);
    assertThat(snapshot.edges().getFirst().metrics().maxMillis()).isEqualTo(140);
  }

  @Test
  void mapsEveryNodeTypeAndEdgeKind() {
    GraphSnapshot proto =
        window()
            .addNodes(node("ext:api.stripe.com", io.github.architrace.grpc.proto.NodeType.NODE_TYPE_EXTERNAL, "api.stripe.com"))
            .addNodes(node("topic:kafka/t", io.github.architrace.grpc.proto.NodeType.NODE_TYPE_TOPIC, "t"))
            .addNodes(node("service:a/b", io.github.architrace.grpc.proto.NodeType.NODE_TYPE_SERVICE, "b"))
            .addEdges(edge("topic:kafka/t", "service:a/b", io.github.architrace.grpc.proto.EdgeKind.EDGE_KIND_CONSUME, 1, 0))
            .build();

    Snapshot snapshot = mapper.toSnapshot(AGENT, proto, RECEIVED);

    assertThat(snapshot.nodes()).extracting(TopologyNode::type).contains(NodeType.EXTERNAL);
    assertThat(snapshot.edges().getFirst().kind()).isEqualTo(EdgeKind.CONSUME);
  }

  @Test
  void rejectsAnEmptyWindow() {
    GraphSnapshot proto =
        GraphSnapshot.newBuilder()
            .setWindowStartEpochMs(WINDOW_END.toEpochMilli())
            .setWindowEndEpochMs(WINDOW_END.toEpochMilli())
            .build();

    assertThatThrownBy(() -> mapper.toSnapshot(AGENT, proto, RECEIVED))
        .isInstanceOf(InvalidSnapshotException.class)
        .hasMessageContaining("window end");
  }

  @Test
  void rejectsUnspecifiedTypeAndKind() {
    GraphSnapshot noType =
        window().addNodes(node("service:a/b", io.github.architrace.grpc.proto.NodeType.NODE_TYPE_UNSPECIFIED, "b")).build();
    GraphSnapshot noKind =
        window()
            .addNodes(node("service:a/b", io.github.architrace.grpc.proto.NodeType.NODE_TYPE_SERVICE, "b"))
            .addNodes(node("db:x/y", io.github.architrace.grpc.proto.NodeType.NODE_TYPE_DATABASE, "y"))
            .addEdges(edge("service:a/b", "db:x/y", io.github.architrace.grpc.proto.EdgeKind.EDGE_KIND_UNSPECIFIED, 1, 0))
            .build();

    assertThatThrownBy(() -> mapper.toSnapshot(AGENT, noType, RECEIVED))
        .isInstanceOf(InvalidSnapshotException.class)
        .hasMessageContaining("node type");
    assertThatThrownBy(() -> mapper.toSnapshot(AGENT, noKind, RECEIVED))
        .isInstanceOf(InvalidSnapshotException.class)
        .hasMessageContaining("edge kind");
  }

  @Test
  void rejectsBlankIdsDuplicatesAndDanglingEdges() {
    GraphSnapshot blank =
        window().addNodes(node("", io.github.architrace.grpc.proto.NodeType.NODE_TYPE_SERVICE, "b")).build();
    GraphSnapshot duplicate =
        window()
            .addNodes(node("service:a/b", io.github.architrace.grpc.proto.NodeType.NODE_TYPE_SERVICE, "b"))
            .addNodes(node("service:a/b", io.github.architrace.grpc.proto.NodeType.NODE_TYPE_SERVICE, "b"))
            .build();
    GraphSnapshot dangling =
        window()
            .addNodes(node("service:a/b", io.github.architrace.grpc.proto.NodeType.NODE_TYPE_SERVICE, "b"))
            .addEdges(edge("service:a/b", "db:x/y", io.github.architrace.grpc.proto.EdgeKind.EDGE_KIND_SYNC, 1, 0))
            .build();
    GraphSnapshot duplicateEdge =
        window()
            .addNodes(node("service:a/b", io.github.architrace.grpc.proto.NodeType.NODE_TYPE_SERVICE, "b"))
            .addNodes(node("db:x/y", io.github.architrace.grpc.proto.NodeType.NODE_TYPE_DATABASE, "y"))
            .addEdges(edge("service:a/b", "db:x/y", io.github.architrace.grpc.proto.EdgeKind.EDGE_KIND_SYNC, 1, 0))
            .addEdges(edge("service:a/b", "db:x/y", io.github.architrace.grpc.proto.EdgeKind.EDGE_KIND_SYNC, 2, 0))
            .build();
    GraphSnapshot negative =
        window()
            .addNodes(node("service:a/b", io.github.architrace.grpc.proto.NodeType.NODE_TYPE_SERVICE, "b"))
            .addNodes(node("db:x/y", io.github.architrace.grpc.proto.NodeType.NODE_TYPE_DATABASE, "y"))
            .addEdges(edge("service:a/b", "db:x/y", io.github.architrace.grpc.proto.EdgeKind.EDGE_KIND_SYNC, -1, 0))
            .build();

    assertThatThrownBy(() -> mapper.toSnapshot(AGENT, blank, RECEIVED))
        .isInstanceOf(InvalidSnapshotException.class)
        .hasMessageContaining("id");
    assertThatThrownBy(() -> mapper.toSnapshot(AGENT, duplicate, RECEIVED))
        .isInstanceOf(InvalidSnapshotException.class)
        .hasMessageContaining("duplicate node");
    assertThatThrownBy(() -> mapper.toSnapshot(AGENT, dangling, RECEIVED))
        .isInstanceOf(InvalidSnapshotException.class)
        .hasMessageContaining("unknown node");
    assertThatThrownBy(() -> mapper.toSnapshot(AGENT, duplicateEdge, RECEIVED))
        .isInstanceOf(InvalidSnapshotException.class)
        .hasMessageContaining("duplicate edge");
    assertThatThrownBy(() -> mapper.toSnapshot(AGENT, negative, RECEIVED))
        .isInstanceOf(InvalidSnapshotException.class)
        .hasMessageContaining("calls");
  }
}
