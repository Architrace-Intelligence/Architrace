/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

public record EdgeMetrics(long calls, long errors, long p50Micros, long p95Micros, long p99Micros, long maxMicros) {

    public EdgeMetrics {
        requireNonNegative(calls, "calls");
        requireNonNegative(errors, "errors");
        requireNonNegative(p50Micros, "p50Micros");
        requireNonNegative(p95Micros, "p95Micros");
        requireNonNegative(p99Micros, "p99Micros");
        requireNonNegative(maxMicros, "maxMicros");
    }

    private static void requireNonNegative(long value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must not be negative: " + value);
        }
    }
}
