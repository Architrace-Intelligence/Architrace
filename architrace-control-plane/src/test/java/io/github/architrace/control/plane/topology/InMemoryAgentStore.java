/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryAgentStore implements AgentStore {

    private final Map<Long, Agent> agents = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public Agent register(AgentRegistration registration, Instant now) {
        Optional<Agent> existing = agents.values().stream()
                .filter(a -> a.name().equals(registration.name()) && a.scope().equals(registration.scope()))
                .findFirst();
        Agent agent = existing.map(
                        a -> new Agent(a.id(), a.name(), registration.version(), a.scope(), a.firstSeenAt(), now))
                .orElseGet(() -> new Agent(
                        new AgentId(nextId++),
                        registration.name(),
                        registration.version(),
                        registration.scope(),
                        now,
                        now));
        agents.put(agent.id().value(), agent);
        return agent;
    }

    @Override
    public void touch(AgentId id, Instant now) {
        agents.computeIfPresent(
                id.value(), (k, a) -> new Agent(a.id(), a.name(), a.version(), a.scope(), a.firstSeenAt(), now));
    }

    @Override
    public Optional<Agent> find(AgentId id) {
        return Optional.ofNullable(agents.get(id.value()));
    }

    @Override
    public List<Agent> all() {
        return List.copyOf(agents.values());
    }
}
