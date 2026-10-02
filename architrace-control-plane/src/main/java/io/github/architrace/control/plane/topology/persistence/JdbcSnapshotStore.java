/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology.persistence;

import io.github.architrace.control.plane.topology.Page;
import io.github.architrace.control.plane.topology.PageRequest;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.Snapshot;
import io.github.architrace.control.plane.topology.SnapshotFilter;
import io.github.architrace.control.plane.topology.SnapshotId;
import io.github.architrace.control.plane.topology.SnapshotStore;
import io.github.architrace.control.plane.topology.SnapshotSummary;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class JdbcSnapshotStore implements SnapshotStore {

    private final SnapshotRepository repository;
    private final SnapshotRows rows = new SnapshotRows(new NodeAttributesCodec());

    JdbcSnapshotStore(SnapshotRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SnapshotId save(Snapshot snapshot) {
        SnapshotRow saved = repository.save(rows.toRow(snapshot));
        return new SnapshotId(saved.id());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Snapshot> find(SnapshotId id) {
        return repository.findById(id.value()).map(rows::toSnapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Snapshot> latestPerAgent(Scope scope, Instant at) {
        return repository.findLatestPerAgent(scope.project(), scope.environment(), scope.cluster(), at).stream()
                .map(rows::toSnapshot)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SnapshotSummary> list(SnapshotFilter filter, PageRequest page) {
        Scope scope = filter.scope();
        Instant from = filter.from().orElse(null);
        Instant to = filter.to().orElse(null);
        List<SnapshotSummary> items = repository
                .findSummaries(
                        scope.project(), scope.environment(), scope.cluster(), from, to, page.size(), page.offset())
                .stream()
                .map(rows::toSummary)
                .toList();
        long total = repository.countSummaries(scope.project(), scope.environment(), scope.cluster(), from, to);
        return new Page<>(items, page, total);
    }

    @Override
    @Transactional
    public int deleteOlderThan(Instant cutoff, int limit) {
        return repository.deleteOlderThan(cutoff, limit);
    }
}
