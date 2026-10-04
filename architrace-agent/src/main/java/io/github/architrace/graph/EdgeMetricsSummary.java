/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

public record EdgeMetricsSummary(
        long calls, long errors, long p50Millis, long p95Millis, long p99Millis, long maxMillis) {}
