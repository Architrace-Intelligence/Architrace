/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
        .withMessageContaining("p99Millis");
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
    assertThat(new AgentRegistration("a", null, new Scope("p", "DEV", "c")).version()).isEmpty();
    assertThatIllegalArgumentException().isThrownBy(() -> new Deployment("", "ns"));
  }

  @Test
  void nodesAndEdgesValidateTheirIdentity() {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> new TopologyNode("", NodeType.SERVICE, "a", NodeAttributes.none()));
    assertThatNullPointerException()
        .isThrownBy(() -> new TopologyNode("service:a/b", null, "b", NodeAttributes.none()));
    assertThatIllegalArgumentException()
        .isThrownBy(
            () -> new TopologyEdge("a", " ", EdgeKind.SYNC, new EdgeMetrics(0, 0, 0, 0, 0, 0)));
    assertThatNullPointerException()
        .isThrownBy(() -> new TopologyEdge("a", "b", EdgeKind.SYNC, null));
  }

  @Test
  void snapshotCopiesItsCollections() {
    List<TopologyNode> nodes =
        new java.util.ArrayList<>(
            List.of(new TopologyNode("ext:x", NodeType.EXTERNAL, "x", NodeAttributes.none())));
    Snapshot snapshot =
        new Snapshot(
            new AgentId(1),
            new Scope("p", "DEV", "c"),
            new TimeWindow(T0, T0.plusSeconds(60)),
            T0,
            nodes,
            List.of());
    nodes.clear();

    assertThat(snapshot.nodes()).hasSize(1);
  }
}
