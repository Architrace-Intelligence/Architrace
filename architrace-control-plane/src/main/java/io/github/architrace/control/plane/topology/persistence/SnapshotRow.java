/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.persistence;

import java.time.Instant;
import java.util.Set;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

@Table("snapshot")
record SnapshotRow(
    @Id Long id,
    long agentId,
    String project,
    String environment,
    String cluster,
    Instant windowStart,
    Instant windowEnd,
    Instant receivedAt,
    int nodeCount,
    int edgeCount,
    @MappedCollection(idColumn = "snapshot_id") Set<SnapshotNodeRow> nodes,
    @MappedCollection(idColumn = "snapshot_id") Set<SnapshotEdgeRow> edges) {}
