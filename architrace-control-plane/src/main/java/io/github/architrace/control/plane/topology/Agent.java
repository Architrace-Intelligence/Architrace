/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import java.time.Instant;

public record Agent(AgentId id, String name, String version, Scope scope, Instant firstSeenAt, Instant lastSeenAt) {}
