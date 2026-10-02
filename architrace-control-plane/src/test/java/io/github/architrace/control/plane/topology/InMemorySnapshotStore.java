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
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class InMemorySnapshotStore implements SnapshotStore {

    private static final Comparator<Map.Entry<Long, Snapshot>> BY_WINDOW_END_THEN_ID =
            Comparator.<Map.Entry<Long, Snapshot>, Instant>comparing(
                            e -> e.getValue().window().end())
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
        return snapshots.entrySet().stream()
                .filter(e -> e.getValue().scope().equals(scope))
                .filter(e -> !e.getValue().window().end().isAfter(at))
                .collect(Collectors.toMap(
                        e -> e.getValue().agentId().value(),
                        Function.identity(),
                        BinaryOperator.maxBy(BY_WINDOW_END_THEN_ID),
                        TreeMap::new))
                .values()
                .stream()
                .map(Map.Entry::getValue)
                .toList();
    }

    @Override
    public Page<SnapshotSummary> list(SnapshotFilter filter, PageRequest page) {
        List<SnapshotSummary> matching = snapshots.entrySet().stream()
                .filter(e -> e.getValue().scope().equals(filter.scope()))
                .filter(e -> filter.includes(e.getValue().window().end()))
                .sorted(BY_WINDOW_END_THEN_ID.reversed())
                .map(e -> summary(e.getKey(), e.getValue()))
                .toList();
        List<SnapshotSummary> items =
                matching.stream().skip(page.offset()).limit(page.size()).toList();
        return new Page<>(items, page, matching.size());
    }

    @Override
    public int deleteOlderThan(Instant cutoff, int limit) {
        List<Long> expired = snapshots.entrySet().stream()
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

    private static SnapshotSummary summary(long id, Snapshot snapshot) {
        return new SnapshotSummary(
                new SnapshotId(id),
                snapshot.agentId(),
                snapshot.scope(),
                snapshot.window(),
                snapshot.receivedAt(),
                snapshot.nodes().size(),
                snapshot.edges().size());
    }
}
