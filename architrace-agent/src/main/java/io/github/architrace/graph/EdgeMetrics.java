/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

public final class EdgeMetrics {

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
                histogram.percentileMicros(0.50),
                histogram.percentileMicros(0.95),
                histogram.percentileMicros(0.99),
                histogram.maxMicros());
    }
}
