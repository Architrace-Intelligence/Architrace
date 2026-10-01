/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.ingestion;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class IngestionMetrics {

  private final Counter ingested;
  private final Counter rejected;
  private final AtomicInteger connected;

  public IngestionMetrics(MeterRegistry registry) {
    this.ingested = registry.counter("architrace.snapshots.ingested");
    this.rejected = registry.counter("architrace.snapshots.rejected");
    this.connected = registry.gauge("architrace.agents.connected", new AtomicInteger());
  }

  public void snapshotIngested() {
    ingested.increment();
  }

  public void snapshotRejected() {
    rejected.increment();
  }

  public void agentConnected() {
    connected.incrementAndGet();
  }

  public void agentDisconnected() {
    connected.decrementAndGet();
  }
}
