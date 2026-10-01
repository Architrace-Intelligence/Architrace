/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.web;

import io.github.architrace.control.plane.api.model.AgentDto;
import io.github.architrace.control.plane.api.model.ScopeDto;
import io.github.architrace.control.plane.api.model.ScopeSummaryDto;
import io.github.architrace.control.plane.topology.AgentStatus;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.ScopeSummary;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

final class ApiModels {

  private ApiModels() {}

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

  private static OffsetDateTime atUtc(Instant instant) {
    return instant.atOffset(ZoneOffset.UTC);
  }
}
