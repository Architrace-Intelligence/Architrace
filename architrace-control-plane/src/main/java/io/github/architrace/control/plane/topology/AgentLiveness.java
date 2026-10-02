/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record AgentLiveness(Duration heartbeatInterval) {

    private static final int MISSED_HEARTBEATS = 3;

    public AgentLiveness {
        Objects.requireNonNull(heartbeatInterval, "heartbeatInterval");
        if (heartbeatInterval.isZero() || heartbeatInterval.isNegative()) {
            throw new IllegalArgumentException("heartbeatInterval must be positive");
        }
    }

    public boolean isLive(Agent agent, Instant now) {
        Duration silence = Duration.between(agent.lastSeenAt(), now);
        return silence.compareTo(heartbeatInterval.multipliedBy(MISSED_HEARTBEATS)) < 0;
    }
}
