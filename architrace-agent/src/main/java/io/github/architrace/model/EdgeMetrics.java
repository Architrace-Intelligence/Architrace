/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.model;

import io.github.architrace.otlp.LatencyHistogram;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public class EdgeMetrics {

  private final LongAdder callCount = new LongAdder();
  private final LongAdder errorCount = new LongAdder();

  private final LongAdder latencySum = new LongAdder();
  private final AtomicLong maxLatency = new AtomicLong();

  private final LatencyHistogram histogram = new LatencyHistogram();

  public void record(long latencyMillis, boolean error) {

    callCount.increment();

    if (error) {
      errorCount.increment();
    }

    latencySum.add(latencyMillis);

    histogram.record(latencyMillis);

    maxLatency.accumulateAndGet(latencyMillis, Math::max);
  }

  public long calls() {
    return callCount.sum();
  }

  public long errors() {
    return errorCount.sum();
  }

  public double errorRate() {
    long calls = callCount.sum();
    return calls == 0 ? 0 : ((double) errorCount.sum() / calls);
  }

  public double avgLatency() {
    long calls = callCount.sum();
    return calls == 0 ? 0 : ((double) latencySum.sum() / calls);
  }

  public long p95() {
    return histogram.p95();
  }

  public long p99() {
    return histogram.p99();
  }

  public static EdgeMetrics merge(EdgeMetrics a, EdgeMetrics b) {

    EdgeMetrics merged = new EdgeMetrics();

    merged.callCount.add(a.callCount.sum() + b.callCount.sum());
    merged.errorCount.add(a.errorCount.sum() + b.errorCount.sum());

    merged.latencySum.add(a.latencySum.sum() + b.latencySum.sum());

    merged.maxLatency.set(
        Math.max(a.maxLatency.get(), b.maxLatency.get())
    );

    merged.histogram.merge(a.histogram);
    merged.histogram.merge(b.histogram);

    return merged;
  }

  public void reset() {
    callCount.reset();
    errorCount.reset();
    latencySum.reset();
    maxLatency.set(0);
  }
}
