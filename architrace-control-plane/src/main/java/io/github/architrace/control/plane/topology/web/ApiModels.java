/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.web;

import io.github.architrace.control.plane.api.model.AgentDto;
import io.github.architrace.control.plane.api.model.DependencyDto;
import io.github.architrace.control.plane.api.model.DeploymentDto;
import io.github.architrace.control.plane.api.model.EdgeKindDto;
import io.github.architrace.control.plane.api.model.EdgeMetricsDto;
import io.github.architrace.control.plane.api.model.NodeTypeDto;
import io.github.architrace.control.plane.api.model.NodeViewDto;
import io.github.architrace.control.plane.api.model.ScopeDto;
import io.github.architrace.control.plane.api.model.ScopeSummaryDto;
import io.github.architrace.control.plane.api.model.SnapshotDto;
import io.github.architrace.control.plane.api.model.SnapshotPageDto;
import io.github.architrace.control.plane.api.model.SnapshotSummaryDto;
import io.github.architrace.control.plane.api.model.TimeWindowDto;
import io.github.architrace.control.plane.api.model.TopologyEdgeDto;
import io.github.architrace.control.plane.api.model.TopologyGraphDto;
import io.github.architrace.control.plane.api.model.TopologyNodeDto;
import io.github.architrace.control.plane.topology.AgentStatus;
import io.github.architrace.control.plane.topology.Dependency;
import io.github.architrace.control.plane.topology.Deployment;
import io.github.architrace.control.plane.topology.EdgeMetrics;
import io.github.architrace.control.plane.topology.NodeView;
import io.github.architrace.control.plane.topology.Page;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.ScopeSummary;
import io.github.architrace.control.plane.topology.Snapshot;
import io.github.architrace.control.plane.topology.SnapshotId;
import io.github.architrace.control.plane.topology.SnapshotSummary;
import io.github.architrace.control.plane.topology.TimeWindow;
import io.github.architrace.control.plane.topology.TopologyEdge;
import io.github.architrace.control.plane.topology.TopologyGraph;
import io.github.architrace.control.plane.topology.TopologyNode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

final class ApiModels {

  private static final Comparator<Deployment> DEPLOYMENT_ORDER =
      Comparator.comparing(Deployment::cluster).thenComparing(Deployment::namespace);

  private ApiModels() {}

  static Optional<Instant> toInstant(OffsetDateTime value) {
    return Optional.ofNullable(value).map(OffsetDateTime::toInstant);
  }

  static ScopeDto toDto(Scope scope) {
    return new ScopeDto(scope.project(), scope.environment(), scope.cluster());
  }

  static ScopeSummaryDto toDto(ScopeSummary summary) {
    return new ScopeSummaryDto(
            toDto(summary.scope()),
            summary.agents(),
            summary.liveAgents(),
            summary.services(),
            summary.dataStreams(),
            summary.namespaces())
        .lastSnapshotAt(summary.lastSnapshotAt().map(ApiModels::atUtc).orElse(null));
  }

  static AgentDto toDto(AgentStatus status) {
    var agent = status.agent();
    return new AgentDto(
        agent.id().value(),
        agent.name(),
        agent.version(),
        toDto(agent.scope()),
        atUtc(agent.firstSeenAt()),
        atUtc(agent.lastSeenAt()),
        status.live());
  }

  static TopologyGraphDto toDto(TopologyGraph graph) {
    return new TopologyGraphDto(
        toDto(graph.scope()),
        atUtc(graph.at()),
        graph.nodes().stream().map(ApiModels::toDto).toList(),
        graph.edges().stream().map(ApiModels::toDto).toList());
  }

  static TopologyNodeDto toDto(TopologyNode node) {
    var attributes = node.attributes();
    return new TopologyNodeDto(
        node.id(),
        NodeTypeDto.fromValue(node.type().name()),
        node.name(),
        attributes.versions().stream().sorted().toList(),
        attributes.deployments().stream().sorted(DEPLOYMENT_ORDER).map(ApiModels::toDto).toList(),
        attributes.labels());
  }

  static DeploymentDto toDto(Deployment deployment) {
    return new DeploymentDto(deployment.cluster(), deployment.namespace());
  }

  static TopologyEdgeDto toDto(TopologyEdge edge) {
    return new TopologyEdgeDto(
        edge.sourceId(),
        edge.targetId(),
        EdgeKindDto.fromValue(edge.kind().name()),
        toDto(edge.metrics()));
  }

  static EdgeMetricsDto toDto(EdgeMetrics metrics) {
    return new EdgeMetricsDto(
        metrics.calls(),
        metrics.errors(),
        metrics.p50Millis(),
        metrics.p95Millis(),
        metrics.p99Millis(),
        metrics.maxMillis());
  }

  static NodeViewDto toDto(NodeView view) {
    return new NodeViewDto(
        toDto(view.node()),
        view.inbound().stream().map(ApiModels::toDto).toList(),
        view.outbound().stream().map(ApiModels::toDto).toList());
  }

  static DependencyDto toDto(Dependency dependency) {
    return new DependencyDto(toDto(dependency.node()), toDto(dependency.edge()));
  }

  static SnapshotPageDto toDto(Page<SnapshotSummary> page) {
    List<SnapshotSummaryDto> items = page.items().stream().map(ApiModels::toDto).toList();
    return new SnapshotPageDto(
        items, page.request().page(), page.request().size(), page.totalItems(), page.totalPages());
  }

  static SnapshotSummaryDto toDto(SnapshotSummary summary) {
    return new SnapshotSummaryDto(
        summary.id().value(),
        summary.agentId().value(),
        toDto(summary.scope()),
        toDto(summary.window()),
        atUtc(summary.receivedAt()),
        summary.nodeCount(),
        summary.edgeCount());
  }

  static SnapshotDto toDto(SnapshotId id, Snapshot snapshot) {
    return new SnapshotDto(
        id.value(),
        snapshot.agentId().value(),
        toDto(snapshot.scope()),
        toDto(snapshot.window()),
        atUtc(snapshot.receivedAt()),
        snapshot.nodes().stream().map(ApiModels::toDto).toList(),
        snapshot.edges().stream().map(ApiModels::toDto).toList());
  }

  static TimeWindowDto toDto(TimeWindow window) {
    return new TimeWindowDto(atUtc(window.start()), atUtc(window.end()));
  }

  private static OffsetDateTime atUtc(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }
}
