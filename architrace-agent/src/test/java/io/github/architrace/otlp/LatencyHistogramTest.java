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
    recordTimes(histogram, 20, 95);
    recordTimes(histogram, 900, 5);

    assertThat(histogram.p95()).isEqualTo(25);
    assertThat(histogram.p99()).isEqualTo(1000);
  }

  @Test
  void latenciesAboveTheLastBucketReportTheLastBucket() {
    LatencyHistogram histogram = new LatencyHistogram();
    recordTimes(histogram, 10_000, 100);

    assertThat(histogram.p95()).isEqualTo(5000);
    assertThat(histogram.p99()).isEqualTo(5000);
  }

  @Test
  void mergeAddsTheCountsOfTheOtherHistogram() {
    LatencyHistogram fast = new LatencyHistogram();
    recordTimes(fast, 20, 95);
    LatencyHistogram slow = new LatencyHistogram();
    recordTimes(slow, 900, 5);

    fast.merge(slow);

    assertThat(fast.p95()).isEqualTo(25);
    assertThat(fast.p99()).isEqualTo(1000);
  }

  private static void recordTimes(LatencyHistogram histogram, long latency, int times) {
    IntStream.range(0, times).forEach(i -> histogram.record(latency));
  }
}
