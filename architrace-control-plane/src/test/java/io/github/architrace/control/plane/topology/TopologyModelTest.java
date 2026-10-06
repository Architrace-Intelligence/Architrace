/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TopologyModelTest {

    private static final Instant T0 = Instant.parse("2026-10-01T12:00:00Z");

    @Test
    void scopeRejectsBlankParts() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Scope(" ", "PROD", "c"))
                .withMessageContaining("project");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Scope("p", null, "c"))
                .withMessageContaining("environment");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Scope("p", "PROD", ""))
                .withMessageContaining("cluster");
    }

    @Test
    void windowEndMustFollowStart() {
        assertThatIllegalArgumentException().isThrownBy(() -> new TimeWindow(T0, T0));
        assertThatIllegalArgumentException().isThrownBy(() -> new TimeWindow(T0, T0.minusSeconds(1)));
        assertThatNullPointerException().isThrownBy(() -> new TimeWindow(null, T0));
        assertThat(new TimeWindow(T0, T0.plusSeconds(60)).end()).isAfter(T0);
    }

    @Test
    void metricsRejectNegativeValues() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EdgeMetrics(-1, 0, 0, 0, 0, 0))
                .withMessageContaining("calls");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EdgeMetrics(1, 0, 0, 0, -5, 0))
                .withMessageContaining("p99Micros");
    }

    @Test
    void attributesAreDefensivelyCopied() {
        Set<String> versions = new HashSet<>(Set.of("1.0.0"));
        Map<String, String> labels = new HashMap<>(Map.of("team", "orders"));
        NodeAttributes attributes = new NodeAttributes(versions, Set.of(), labels);
        versions.add("2.0.0");
        labels.put("owner", "x");

        assertThat(attributes.versions()).containsExactly("1.0.0");
        assertThat(attributes.labels()).containsOnlyKeys("team");
        assertThat(NodeAttributes.none().deployments()).isEmpty();
    }

    @Test
    void optionalTextDefaultsToEmpty() {
        assertThat(new Deployment("k8s-dev", null).namespace()).isEmpty();
        assertThat(new AgentRegistration("a", null, new Scope("p", "DEV", "c")).version())
                .isEmpty();
        assertThatIllegalArgumentException().isThrownBy(() -> new Deployment("", "ns"));
    }

    @Test
    void nodesAndEdgesValidateTheirIdentity() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TopologyNode("", NodeType.SERVICE, "a", NodeAttributes.none()));
        assertThatNullPointerException()
                .isThrownBy(() -> new TopologyNode("service:a/b", null, "b", NodeAttributes.none()));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new TopologyEdge("a", " ", EdgeKind.SYNC, new EdgeMetrics(0, 0, 0, 0, 0, 0)));
        assertThatNullPointerException().isThrownBy(() -> new TopologyEdge("a", "b", EdgeKind.SYNC, null));
    }

    @Test
    void snapshotCopiesItsCollections() {
        List<TopologyNode> nodes = new java.util.ArrayList<>(
                List.of(new TopologyNode("ext:x", NodeType.EXTERNAL, "x", NodeAttributes.none())));
        Snapshot snapshot = new Snapshot(
                new AgentId(1),
                new Scope("p", "DEV", "c"),
                new TimeWindow(T0, T0.plusSeconds(60)),
                T0,
                nodes,
                List.of());
        nodes.clear();

        assertThat(snapshot.nodes()).hasSize(1);
    }

    @Test
    void topologyGraphCopiesCollectionsAndRejectsNulls() {
        Scope scope = new Scope("p", "PROD", "c");
        List<TopologyNode> nodes =
                new ArrayList<>(List.of(new TopologyNode("service:a", NodeType.SERVICE, "a", NodeAttributes.none())));
        TopologyGraph graph = new TopologyGraph(scope, T0, nodes, List.of());
        nodes.clear();

        assertThat(graph.nodes()).hasSize(1);
        assertThatNullPointerException().isThrownBy(() -> new TopologyGraph(null, T0, List.of(), List.of()));
        assertThatNullPointerException().isThrownBy(() -> new TopologyGraph(scope, null, List.of(), List.of()));
    }

    @Test
    void scopeSummaryRejectsNulls() {
        assertThatNullPointerException().isThrownBy(() -> new ScopeSummary(null, 0, 0, 0, 0, 0, Optional.empty()));
        assertThatNullPointerException()
                .isThrownBy(() -> new ScopeSummary(new Scope("p", "PROD", "c"), 0, 0, 0, 0, 0, null));
    }

    @Test
    void pageRequestValidatesItsBounds() {
        assertThatExceptionOfType(InvalidQueryException.class)
                .isThrownBy(() -> new PageRequest(-1, 10))
                .withMessageContaining("page");
        assertThatExceptionOfType(InvalidQueryException.class)
                .isThrownBy(() -> new PageRequest(0, 0))
                .withMessageContaining("size");
        assertThatExceptionOfType(InvalidQueryException.class)
                .isThrownBy(() -> new PageRequest(0, PageRequest.MAX_SIZE + 1));
        assertThat(new PageRequest(3, 20).offset()).isEqualTo(60);
    }

    @Test
    void snapshotFilterRejectsAnInvertedRangeAndMatchesWindowEndsInclusively() {
        Scope scope = new Scope("p", "PROD", "c");
        Optional<Instant> start = Optional.of(T0);
        Optional<Instant> earlierEnd = Optional.of(T0.minusSeconds(1));
        assertThatExceptionOfType(InvalidQueryException.class)
                .isThrownBy(() -> new SnapshotFilter(scope, start, earlierEnd))
                .withMessageContaining("from");
        assertThatNullPointerException().isThrownBy(() -> new SnapshotFilter(scope, null, Optional.empty()));

        SnapshotFilter bounded = new SnapshotFilter(scope, Optional.of(T0), Optional.of(T0.plusSeconds(60)));
        SnapshotFilter from = new SnapshotFilter(scope, Optional.of(T0), Optional.empty());
        SnapshotFilter to = new SnapshotFilter(scope, Optional.empty(), Optional.of(T0));

        assertThat(bounded.includes(T0)).isTrue();
        assertThat(bounded.includes(T0.plusSeconds(60))).isTrue();
        assertThat(bounded.includes(T0.minusSeconds(1))).isFalse();
        assertThat(bounded.includes(T0.plusSeconds(61))).isFalse();
        assertThat(from.includes(T0.minusSeconds(1))).isFalse();
        assertThat(from.includes(T0.plusSeconds(3600))).isTrue();
        assertThat(to.includes(T0.plusSeconds(1))).isFalse();
        assertThat(SnapshotFilter.all(scope).includes(Instant.EPOCH)).isTrue();
    }

    @Test
    void pageCountsItsPagesAndCopiesItsItems() {
        List<String> items = new ArrayList<>(List.of("a"));
        Page<String> page = new Page<>(items, new PageRequest(0, 2), 5);
        items.clear();

        assertThat(page.items()).containsExactly("a");
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(new Page<>(List.of(), new PageRequest(0, 2), 0).totalPages()).isZero();
        assertThatIllegalArgumentException().isThrownBy(() -> new Page<>(List.of(), new PageRequest(0, 2), -1));
        assertThatNullPointerException().isThrownBy(() -> new Page<>(List.of(), null, 0));
    }

    @Test
    void viewsAndSummariesRejectNulls() {
        TopologyNode node = new TopologyNode("service:a", NodeType.SERVICE, "a", NodeAttributes.none());
        TopologyEdge edge = new TopologyEdge("a", "b", EdgeKind.SYNC, new EdgeMetrics(0, 0, 0, 0, 0, 0));
        assertThatNullPointerException().isThrownBy(() -> new NodeView(null, List.of(), List.of()));
        assertThatNullPointerException().isThrownBy(() -> new Dependency(null, edge));
        assertThatNullPointerException().isThrownBy(() -> new Dependency(node, null));
        assertThatNullPointerException()
                .isThrownBy(() -> new SnapshotSummary(
                        null,
                        new AgentId(1),
                        new Scope("p", "PROD", "c"),
                        new TimeWindow(T0, T0.plusSeconds(60)),
                        T0,
                        0,
                        0));
        assertThat(new NodeView(node, List.of(), List.of()).inbound()).isEmpty();
    }
}
