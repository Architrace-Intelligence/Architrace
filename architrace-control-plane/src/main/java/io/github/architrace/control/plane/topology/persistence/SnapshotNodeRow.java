/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology.persistence;

import org.springframework.data.relational.core.mapping.Table;

@Table("snapshot_node")
record SnapshotNodeRow(String nodeId, String type, String name, JsonDocument attributes) {}
