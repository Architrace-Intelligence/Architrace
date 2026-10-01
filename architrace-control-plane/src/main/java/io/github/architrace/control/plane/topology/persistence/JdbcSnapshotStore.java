/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.persistence;

import io.github.architrace.control.plane.topology.Snapshot;
import io.github.architrace.control.plane.topology.SnapshotId;
import io.github.architrace.control.plane.topology.SnapshotStore;
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
}
