/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SnapshotStore {

  SnapshotId save(Snapshot snapshot);

  Optional<Snapshot> find(SnapshotId id);

  List<Snapshot> latestPerAgent(Scope scope, Instant at);

  Page<SnapshotSummary> list(SnapshotFilter filter, PageRequest page);

  int deleteOlderThan(Instant cutoff, int limit);
}
