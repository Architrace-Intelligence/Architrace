/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.persistence;

import org.springframework.data.repository.CrudRepository;

interface SnapshotRepository extends CrudRepository<SnapshotRow, Long> {}
