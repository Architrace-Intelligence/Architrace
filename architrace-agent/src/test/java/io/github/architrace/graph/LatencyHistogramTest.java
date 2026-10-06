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
        assertThat(histogram.percentileMicros(0.5)).isZero();
        assertThat(histogram.maxMicros()).isZero();
    }

    @Test
    void percentilesAreEstimatedFromLogarithmicBucketUpperBounds() {
        LatencyHistogram histogram = new LatencyHistogram();
        LongStream.rangeClosed(1, 100).forEach(histogram::observe);

        assertThat(histogram.total()).isEqualTo(100);
        assertThat(histogram.percentileMicros(0.50)).isEqualTo(64);
        assertThat(histogram.percentileMicros(0.95)).isEqualTo(100);
        assertThat(histogram.percentileMicros(0.99)).isEqualTo(100);
        assertThat(histogram.maxMicros()).isEqualTo(100);
    }

    @Test
    void subMillisecondLatenciesKeepTheirResolution() {
        LatencyHistogram histogram = new LatencyHistogram();
        LongStream.generate(() -> 350).limit(9).forEach(histogram::observe);
        histogram.observe(900);

        assertThat(histogram.percentileMicros(0.50)).isEqualTo(512);
        assertThat(histogram.percentileMicros(0.99)).isEqualTo(900);
        assertThat(histogram.maxMicros()).isEqualTo(900);
    }

    @Test
    void singleObservationIsBoundedByTheObservedMaximum() {
        LatencyHistogram histogram = new LatencyHistogram();
        histogram.observe(10);

        assertThat(histogram.percentileMicros(0.5)).isEqualTo(10);
        assertThat(histogram.percentileMicros(0.99)).isEqualTo(10);
    }

    @Test
    void latenciesBeyondTheLastBucketReportTheMaximum() {
        LatencyHistogram histogram = new LatencyHistogram();
        histogram.observe(1);
        histogram.observe(250_000_000);

        assertThat(histogram.percentileMicros(0.5)).isEqualTo(1);
        assertThat(histogram.percentileMicros(0.99)).isEqualTo(250_000_000);
    }

    @Test
    void edgeMetricsSummariseCallsErrorsAndLatenciesInWholeMillis() {
        EdgeMetrics metrics = new EdgeMetrics();
        metrics.observe(5_000, false);
        metrics.observe(40_000, true);
        metrics.observe(900_000, false);

        assertThat(metrics.summary()).isEqualTo(new EdgeMetricsSummary(3, 1, 65, 900, 900, 900));
    }

    @Test
    void edgeMetricsTruncateSubMillisecondLatenciesToZero() {
        EdgeMetrics metrics = new EdgeMetrics();
        metrics.observe(350, false);

        assertThat(metrics.summary()).isEqualTo(new EdgeMetricsSummary(1, 0, 0, 0, 0, 0));
    }
}
