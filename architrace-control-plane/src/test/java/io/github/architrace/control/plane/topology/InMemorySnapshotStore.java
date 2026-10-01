/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemorySnapshotStore implements SnapshotStore {

  private final Map<Long, Snapshot> snapshots = new LinkedHashMap<>();
  private long nextId = 1;

  @Override
  public SnapshotId save(Snapshot snapshot) {
    SnapshotId id = new SnapshotId(nextId++);
    snapshots.put(id.value(), snapshot);
    return id;
  }

  @Override
  public Optional<Snapshot> find(SnapshotId id) {
    return Optional.ofNullable(snapshots.get(id.value()));
  }

  public List<Snapshot> all() {
    return List.copyOf(snapshots.values());
  }
}
