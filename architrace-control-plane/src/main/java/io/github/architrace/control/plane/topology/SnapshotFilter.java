/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record SnapshotFilter(Scope scope, Optional<Instant> from, Optional<Instant> to) {

  public SnapshotFilter {
    Objects.requireNonNull(scope, "scope");
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    if (from.isPresent() && to.isPresent() && from.get().isAfter(to.get())) {
      throw new InvalidQueryException("from " + from.get() + " must not be after to " + to.get());
    }
  }

  public static SnapshotFilter all(Scope scope) {
    return new SnapshotFilter(scope, Optional.empty(), Optional.empty());
  }

  public boolean includes(Instant windowEnd) {
    return from.map(bound -> !windowEnd.isBefore(bound)).orElse(true)
        && to.map(bound -> !windowEnd.isAfter(bound)).orElse(true);
  }
}
