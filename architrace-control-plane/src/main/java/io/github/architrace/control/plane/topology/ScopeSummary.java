/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record ScopeSummary(
    Scope scope,
    int agents,
    int liveAgents,
    int services,
    int dataStreams,
    int namespaces,
    Optional<Instant> lastSnapshotAt) {

  public ScopeSummary {
    Objects.requireNonNull(scope, "scope");
    Objects.requireNonNull(lastSnapshotAt, "lastSnapshotAt");
  }
}
