/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AgentStore {

    Agent register(AgentRegistration registration, Instant now);

    void touch(AgentId id, Instant now);

    Optional<Agent> find(AgentId id);

    List<Agent> all();
}
