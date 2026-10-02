/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import java.time.Instant;
import java.util.Objects;

public record SnapshotSummary(
        SnapshotId id,
        AgentId agentId,
        Scope scope,
        TimeWindow window,
        Instant receivedAt,
        int nodeCount,
        int edgeCount) {

    public SnapshotSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(agentId, "agentId");
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(receivedAt, "receivedAt");
    }
}
