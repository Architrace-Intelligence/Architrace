/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.otlp;

import java.util.Arrays;
import java.util.OptionalInt;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class LatencyHistogram {

  private static final long[] BUCKETS = {5, 10, 25, 50, 100, 250, 500, 1000, 2000, 5000};

  private final LongAdder[] counts =
      Stream.generate(LongAdder::new).limit(BUCKETS.length + 1L).toArray(LongAdder[]::new);

  public void record(long latency) {
    int bucket =
        IntStream.range(0, BUCKETS.length)
            .filter(i -> latency <= BUCKETS[i])
            .findFirst()
            .orElse(BUCKETS.length);
    counts[bucket].increment();
  }

  public long p95() {
    return percentile(0.95);
  }

  public long p99() {
    return percentile(0.99);
  }

  private long percentile(double p) {
    long[] cumulative = Arrays.stream(counts).mapToLong(LongAdder::sum).toArray();
    Arrays.parallelPrefix(cumulative, Long::sum);
    long target = (long) (cumulative[cumulative.length - 1] * p);
    OptionalInt bucket = IntStream.range(0, cumulative.length).filter(i -> cumulative[i] >= target).findFirst();
    return bucket.isPresent() ? BUCKETS[Math.min(bucket.getAsInt(), BUCKETS.length - 1)] : 0;
  }

  public void merge(LatencyHistogram other) {
    IntStream.range(0, counts.length).forEach(i -> counts[i].add(other.counts[i].sum()));
  }
}
