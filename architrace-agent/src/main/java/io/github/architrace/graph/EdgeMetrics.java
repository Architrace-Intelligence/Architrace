/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

public final class EdgeMetrics {

    private static final long MICROS_PER_MILLI = 1_000L;

    private final LatencyHistogram histogram = new LatencyHistogram();
    private long errors;

    public void observe(long latencyMicros, boolean error) {
        histogram.observe(latencyMicros);
        if (error) {
            errors++;
        }
    }

    public EdgeMetricsSummary summary() {
        return new EdgeMetricsSummary(
                histogram.total(),
                errors,
                millis(histogram.percentileMicros(0.50)),
                millis(histogram.percentileMicros(0.95)),
                millis(histogram.percentileMicros(0.99)),
                millis(histogram.maxMicros()));
    }

    private static long millis(long micros) {
        return micros / MICROS_PER_MILLI;
    }
}
