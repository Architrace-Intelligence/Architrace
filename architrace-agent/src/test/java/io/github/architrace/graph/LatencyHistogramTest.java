/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class LatencyHistogramTest {

    @Test
    void emptyHistogramReportsZero() {
        LatencyHistogram histogram = new LatencyHistogram();

        assertThat(histogram.total()).isZero();
        assertThat(histogram.percentileMillis(0.5)).isZero();
        assertThat(histogram.maxMillis()).isZero();
    }

    @Test
    void percentilesAreEstimatedFromLogarithmicBucketUpperBounds() {
        LatencyHistogram histogram = new LatencyHistogram();
        LongStream.rangeClosed(1, 100).forEach(histogram::record);

        assertThat(histogram.total()).isEqualTo(100);
        assertThat(histogram.percentileMillis(0.50)).isEqualTo(64);
        assertThat(histogram.percentileMillis(0.95)).isEqualTo(100);
        assertThat(histogram.percentileMillis(0.99)).isEqualTo(100);
        assertThat(histogram.maxMillis()).isEqualTo(100);
    }

    @Test
    void singleObservationIsBoundedByTheObservedMaximum() {
        LatencyHistogram histogram = new LatencyHistogram();
        histogram.record(10);

        assertThat(histogram.percentileMillis(0.5)).isEqualTo(10);
        assertThat(histogram.percentileMillis(0.99)).isEqualTo(10);
    }

    @Test
    void latenciesBeyondTheLastBucketReportTheMaximum() {
        LatencyHistogram histogram = new LatencyHistogram();
        histogram.record(1);
        histogram.record(250_000);

        assertThat(histogram.percentileMillis(0.5)).isEqualTo(1);
        assertThat(histogram.percentileMillis(0.99)).isEqualTo(250_000);
    }

    @Test
    void edgeMetricsSummariseCallsErrorsAndLatencies() {
        EdgeMetrics metrics = new EdgeMetrics();
        metrics.record(5, false);
        metrics.record(40, true);
        metrics.record(900, false);

        assertThat(metrics.summary()).isEqualTo(new EdgeMetricsSummary(3, 1, 64, 900, 900, 900));
    }
}
