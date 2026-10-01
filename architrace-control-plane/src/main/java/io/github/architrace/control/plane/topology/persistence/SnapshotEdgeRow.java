/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.persistence;

import org.springframework.data.relational.core.mapping.Table;

@Table("snapshot_edge")
record SnapshotEdgeRow(
    String sourceId,
    String targetId,
    String kind,
    long calls,
    long errors,
    long latencyP50Ms,
    long latencyP95Ms,
    long latencyP99Ms,
    long latencyMaxMs) {}
