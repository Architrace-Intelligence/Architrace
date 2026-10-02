/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import java.util.Objects;

public record AgentStatus(Agent agent, boolean live) {

    public AgentStatus {
        Objects.requireNonNull(agent, "agent");
    }
}
