/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules.persistence;

import io.github.architrace.control.plane.topology.persistence.JsonDocument;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("finding")
record FindingRow(
        @Id Long id,
        String project,
        String environment,
        String cluster,
        String ruleId,
        String severity,
        JsonDocument subjectIds,
        String title,
        String detail,
        JsonDocument evidence,
        Instant evaluatedAt) {}
