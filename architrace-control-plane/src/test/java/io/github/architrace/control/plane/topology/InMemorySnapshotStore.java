/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

public final class InMemorySnapshotStore implements SnapshotStore {

  private static final Comparator<Map.Entry<Long, Snapshot>> BY_WINDOW_END_THEN_ID =
      Comparator.<Map.Entry<Long, Snapshot>, Instant>comparing(e -> e.getValue().window().end())
          .thenComparing(Map.Entry::getKey);

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

  @Override
  public List<Snapshot> latestPerAgent(Scope scope, Instant at) {
    Map<Long, Map.Entry<Long, Snapshot>> latest = new TreeMap<>();
    for (Map.Entry<Long, Snapshot> entry : snapshots.entrySet()) {
      Snapshot snapshot = entry.getValue();
      if (snapshot.scope().equals(scope) && !snapshot.window().end().isAfter(at)) {
        latest.merge(
            snapshot.agentId().value(),
            entry,
            (a, b) -> BY_WINDOW_END_THEN_ID.compare(a, b) >= 0 ? a : b);
      }
    }
    return latest.values().stream().map(Map.Entry::getValue).toList();
  }

  @Override
  public int deleteOlderThan(Instant cutoff, int limit) {
    List<Long> expired =
        snapshots.entrySet().stream()
            .filter(e -> e.getValue().window().end().isBefore(cutoff))
            .sorted(BY_WINDOW_END_THEN_ID)
            .limit(limit)
            .map(Map.Entry::getKey)
            .toList();
    expired.forEach(snapshots::remove);
    return expired.size();
  }

  public List<Snapshot> all() {
    return List.copyOf(snapshots.values());
  }
}
