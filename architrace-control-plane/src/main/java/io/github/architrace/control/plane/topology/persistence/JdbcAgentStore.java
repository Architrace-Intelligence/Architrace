/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.persistence;

import io.github.architrace.control.plane.topology.Agent;
import io.github.architrace.control.plane.topology.AgentId;
import io.github.architrace.control.plane.topology.AgentRegistration;
import io.github.architrace.control.plane.topology.AgentStore;
import io.github.architrace.control.plane.topology.Scope;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class JdbcAgentStore implements AgentStore {

  private final AgentRepository repository;

  JdbcAgentStore(AgentRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public Agent register(AgentRegistration registration, Instant now) {
    Scope scope = registration.scope();
    AgentRow row =
        repository
            .findByNameAndProjectAndEnvironmentAndCluster(
                registration.name(), scope.project(), scope.environment(), scope.cluster())
            .map(
                existing ->
                    new AgentRow(
                        existing.id(),
                        existing.name(),
                        registration.version(),
                        existing.project(),
                        existing.environment(),
                        existing.cluster(),
                        existing.firstSeenAt(),
                        now))
            .orElseGet(
                () ->
                    new AgentRow(
                        null,
                        registration.name(),
                        registration.version(),
                        scope.project(),
                        scope.environment(),
                        scope.cluster(),
                        now,
                        now));
    return toAgent(repository.save(row));
  }

  @Override
  @Transactional
  public void touch(AgentId id, Instant now) {
    repository.touch(id.value(), now);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Agent> find(AgentId id) {
    return repository.findById(id.value()).map(JdbcAgentStore::toAgent);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Agent> all() {
    return repository.findAll().stream().map(JdbcAgentStore::toAgent).toList();
  }

  private static Agent toAgent(AgentRow row) {
    return new Agent(
        new AgentId(row.id()),
        row.name(),
        row.agentVersion(),
        new Scope(row.project(), row.environment(), row.cluster()),
        row.firstSeenAt(),
        row.lastSeenAt());
  }
}
