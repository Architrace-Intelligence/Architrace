/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.retention;

import io.github.architrace.control.plane.topology.SnapshotStore;
import io.github.architrace.control.plane.topology.TopologyMetrics;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RetentionJob {

  private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);

  private final SnapshotStore snapshots;
  private final RetentionProperties properties;
  private final TopologyMetrics metrics;
  private final Clock clock;

  public RetentionJob(
      SnapshotStore snapshots,
      RetentionProperties properties,
      TopologyMetrics metrics,
      Clock clock) {
    this.snapshots = snapshots;
    this.properties = properties;
    this.metrics = metrics;
    this.clock = clock;
  }

  @Scheduled(cron = "${architrace.topology.retention.cron:0 0 3 * * *}", zone = "UTC")
  public void run() {
    Instant cutoff = clock.instant().minus(properties.period());
    int total = 0;
    int deleted;
    do {
      deleted = snapshots.deleteOlderThan(cutoff, properties.batchSize());
      total += deleted;
    } while (deleted == properties.batchSize());
    metrics.snapshotsDeleted(total);
    log.info("Retention removed {} snapshots whose window ended before {}", total, cutoff);
  }
}
