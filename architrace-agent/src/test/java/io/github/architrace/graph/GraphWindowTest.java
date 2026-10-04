/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.graph.GraphNode.DatabaseNode;
import io.github.architrace.graph.GraphNode.ServiceNode;
import io.github.architrace.span.Deployment;
import io.github.architrace.span.Peer;
import io.github.architrace.span.ServiceIdentity;
import io.github.architrace.span.SpanKind;
import io.github.architrace.span.SpanRecord;
import io.github.architrace.testsupport.TestSpans;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GraphWindowTest {

    private static final Instant START = Instant.parse("2026-10-04T10:00:00Z");
    private static final Instant END = START.plusSeconds(60);
    private static final ServiceNode CHECKOUT = new ServiceNode("default", "checkout");
    private static final DatabaseNode ORDERS_DB = new DatabaseNode("postgresql", Optional.of("orders"));

    private final GraphWindow sut = new GraphWindow(START);

    @Test
    void serviceNodesAccumulateVersionsAndDeployments() {
        sut.recordService(span("1.0", "eu-1", "checkout"));
        sut.recordService(span("1.1", "eu-1", "checkout"));
        sut.recordService(span("1.1", "eu-2", "checkout"));

        GraphSnapshot snapshot = sut.freeze(END);

        assertThat(snapshot.windowStart()).isEqualTo(START);
        assertThat(snapshot.windowEnd()).isEqualTo(END);
        assertThat(snapshot.nodes())
                .containsExactly(new SnapshotNode(
                        CHECKOUT,
                        Set.of("1.0", "1.1"),
                        Set.of(
                                new Placement("eu-1", Optional.of("checkout")),
                                new Placement("eu-2", Optional.of("checkout")))));
        assertThat(snapshot.edges()).isEmpty();
    }

    @Test
    void edgeObservationsCreateTheirNodesAndAggregateMetrics() {
        EdgeKey key = new EdgeKey(CHECKOUT, ORDERS_DB, EdgeKind.SYNC);
        sut.observe(new EdgeObservation(key, 10, false));
        sut.observe(new EdgeObservation(key, 30, true));

        GraphSnapshot snapshot = sut.freeze(END);

        assertThat(snapshot.nodes()).extracting(SnapshotNode::node).containsExactly(CHECKOUT, ORDERS_DB);
        assertThat(snapshot.nodes()).allSatisfy(node -> {
            assertThat(node.versions()).isEmpty();
            assertThat(node.deployments()).isEmpty();
        });
        assertThat(snapshot.edges())
                .containsExactly(new SnapshotEdge(key, new EdgeMetricsSummary(2, 1, 16, 30, 30, 30)));
        assertThat(snapshot.isEmpty()).isFalse();
    }

    @Test
    void frozenSnapshotIsImmutable() {
        GraphSnapshot snapshot = new GraphWindow(START).freeze(END);

        assertThat(snapshot.isEmpty()).isTrue();
        assertThat(snapshot.nodes()).isUnmodifiable();
        assertThat(snapshot.edges()).isUnmodifiable();
    }

    private static SpanRecord span(String version, String cluster, String namespace) {
        return new SpanRecord(
                "t",
                "s",
                Optional.empty(),
                SpanKind.SERVER,
                0,
                0,
                false,
                new ServiceIdentity(TestSpans.ENVIRONMENT, "default", "checkout", version),
                new Deployment(cluster, Optional.of(namespace), Optional.of("pod-1")),
                new Peer.None());
    }
}
