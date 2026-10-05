/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.ingestion;

import io.github.architrace.control.plane.topology.Scope;
import java.time.Instant;
import java.util.Objects;

public record SnapshotIngested(Scope scope, Instant receivedAt) {

    public SnapshotIngested {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(receivedAt, "receivedAt");
    }
}
