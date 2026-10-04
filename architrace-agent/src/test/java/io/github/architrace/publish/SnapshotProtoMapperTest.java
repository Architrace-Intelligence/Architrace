/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.publish;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import io.github.architrace.grpc.proto.Deployment;
import io.github.architrace.grpc.proto.EdgeKind;
import io.github.architrace.grpc.proto.GraphSnapshot;
import io.github.architrace.grpc.proto.NodeType;
import io.github.architrace.grpc.proto.SnapshotEdge;
import io.github.architrace.grpc.proto.SnapshotNode;
import io.github.architrace.testsupport.TestSnapshots;
import org.junit.jupiter.api.Test;

class SnapshotProtoMapperTest {

    private final GraphSnapshot proto = SnapshotProtoMapper.toProto(TestSnapshots.full());

    @Test
    void windowIsMappedToEpochMillis() {
        assertThat(proto.getWindowStartEpochMs()).isEqualTo(TestSnapshots.START.toEpochMilli());
        assertThat(proto.getWindowEndEpochMs()).isEqualTo(TestSnapshots.END.toEpochMilli());
    }

    @Test
    void nodesCarryIdTypeNameVersionsAndDeployments() {
        assertThat(proto.getNodesList())
                .extracting(SnapshotNode::getId, SnapshotNode::getType, SnapshotNode::getName)
                .containsExactly(
                        tuple("service:shop/checkout", NodeType.NODE_TYPE_SERVICE, "checkout"),
                        tuple("service:shop/orders", NodeType.NODE_TYPE_SERVICE, "orders"),
                        tuple("db:postgresql/orders", NodeType.NODE_TYPE_DATABASE, "orders"),
                        tuple("topic:kafka/orders", NodeType.NODE_TYPE_TOPIC, "orders"),
                        tuple("ext:payments.example.com", NodeType.NODE_TYPE_EXTERNAL, "payments.example.com"));
        SnapshotNode checkout = proto.getNodes(0);
        assertThat(checkout.getVersionsList()).containsExactly("1.4.1", "1.4.2");
        assertThat(checkout.getDeploymentsList())
                .containsExactly(
                        Deployment.newBuilder()
                                .setCluster("eu-1")
                                .setNamespace("checkout")
                                .build(),
                        Deployment.newBuilder()
                                .setCluster("eu-2")
                                .setNamespace("")
                                .build());
        assertThat(checkout.getLabelsMap()).isEmpty();
    }

    @Test
    void edgesCarryEndpointsKindAndMetrics() {
        assertThat(proto.getEdgesList())
                .extracting(SnapshotEdge::getSourceId, SnapshotEdge::getTargetId, SnapshotEdge::getKind)
                .containsExactly(
                        tuple("service:shop/checkout", "service:shop/orders", EdgeKind.EDGE_KIND_SYNC),
                        tuple("service:shop/orders", "db:postgresql/orders", EdgeKind.EDGE_KIND_SYNC),
                        tuple("service:shop/checkout", "topic:kafka/orders", EdgeKind.EDGE_KIND_PUBLISH),
                        tuple("topic:kafka/orders", "service:shop/orders", EdgeKind.EDGE_KIND_CONSUME),
                        tuple("service:shop/checkout", "ext:payments.example.com", EdgeKind.EDGE_KIND_SYNC));
        io.github.architrace.grpc.proto.EdgeMetrics metrics = proto.getEdges(0).getMetrics();
        assertThat(metrics.getCalls()).isEqualTo(10);
        assertThat(metrics.getErrors()).isEqualTo(1);
        assertThat(metrics.getP50Millis()).isEqualTo(8);
        assertThat(metrics.getP95Millis()).isEqualTo(32);
        assertThat(metrics.getP99Millis()).isEqualTo(64);
        assertThat(metrics.getMaxMillis()).isEqualTo(70);
    }
}
