/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.otlp;

import java.util.concurrent.atomic.LongAdder;

public class LatencyHistogram {

  private static final long[] BUCKETS = {
      5,10,25,50,100,250,500,1000,2000,5000
  };

  private final LongAdder[] counts;

  public LatencyHistogram() {
    counts = new LongAdder[BUCKETS.length + 1];
    for (int i = 0; i < counts.length; i++) {
      counts[i] = new LongAdder();
    }
  }

  public void record(long latency) {

    for (int i = 0; i < BUCKETS.length; i++) {
      if (latency <= BUCKETS[i]) {
        counts[i].increment();
        return;
      }
    }

    counts[counts.length - 1].increment();
  }

  public long p95() {
    return percentile(0.95);
  }

  public long p99() {
    return percentile(0.99);
  }

  private long percentile(double p) {

    long total = 0;

    for (LongAdder c : counts) {
      total += c.sum();
    }

    long target = (long) (total * p);

    long cumulative = 0;

    for (int i = 0; i < counts.length; i++) {

      cumulative += counts[i].sum();

      if (cumulative >= target) {

        if (i < BUCKETS.length) {
          return BUCKETS[i];
        }

        return BUCKETS[BUCKETS.length - 1];
      }
    }

    return 0;
  }

  public void merge(LatencyHistogram other) {

    for (int i = 0; i < counts.length; i++) {
      counts[i].add(other.counts[i].sum());
    }
  }
}