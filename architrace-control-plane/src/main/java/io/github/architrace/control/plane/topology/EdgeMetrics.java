/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

public record EdgeMetrics(long calls, long errors, long p50Millis, long p95Millis, long p99Millis, long maxMillis) {

    public EdgeMetrics {
        requireNonNegative(calls, "calls");
        requireNonNegative(errors, "errors");
        requireNonNegative(p50Millis, "p50Millis");
        requireNonNegative(p95Millis, "p95Millis");
        requireNonNegative(p99Millis, "p99Millis");
        requireNonNegative(maxMillis, "maxMillis");
    }

    private static void requireNonNegative(long value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must not be negative: " + value);
        }
    }
}
