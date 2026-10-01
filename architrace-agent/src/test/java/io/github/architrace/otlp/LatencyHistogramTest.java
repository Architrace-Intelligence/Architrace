/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.otlp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class LatencyHistogramTest {

  @Test
  void percentilesReportTheUpperBoundOfTheBucketReached() {
    LatencyHistogram histogram = new LatencyHistogram();
    record(histogram, 20, 95);
    record(histogram, 900, 5);

    assertThat(histogram.p95()).isEqualTo(25);
    assertThat(histogram.p99()).isEqualTo(1000);
  }

  @Test
  void latenciesAboveTheLastBucketReportTheLastBucket() {
    LatencyHistogram histogram = new LatencyHistogram();
    record(histogram, 10_000, 100);

    assertThat(histogram.p95()).isEqualTo(5000);
    assertThat(histogram.p99()).isEqualTo(5000);
  }

  @Test
  void mergeAddsTheCountsOfTheOtherHistogram() {
    LatencyHistogram fast = new LatencyHistogram();
    record(fast, 20, 95);
    LatencyHistogram slow = new LatencyHistogram();
    record(slow, 900, 5);

    fast.merge(slow);

    assertThat(fast.p95()).isEqualTo(25);
    assertThat(fast.p99()).isEqualTo(1000);
  }

  private static void record(LatencyHistogram histogram, long latency, int times) {
    IntStream.range(0, times).forEach(i -> histogram.record(latency));
  }
}
