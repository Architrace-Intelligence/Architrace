/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.persistence;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("agent")
record AgentRow(
    @Id Long id,
    String name,
    String agentVersion,
    String project,
    String environment,
    String cluster,
    Instant firstSeenAt,
    Instant lastSeenAt) {}
