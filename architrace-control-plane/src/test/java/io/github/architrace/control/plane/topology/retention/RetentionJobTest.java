/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.retention;

import static io.github.architrace.control.plane.topology.TestTopology.NOW;
import static io.github.architrace.control.plane.topology.TestTopology.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.architrace.control.plane.topology.AgentId;
import io.github.architrace.control.plane.topology.InMemorySnapshotStore;
import io.github.architrace.control.plane.topology.Snapshot;
import io.github.architrace.control.plane.topology.TopologyMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class RetentionJobTest {

  private static final AgentId AGENT = new AgentId(1);
  private static final Duration PERIOD = Duration.ofDays(30);

  private final InMemorySnapshotStore snapshots = new InMemorySnapshotStore();
  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final RetentionJob job =
      new RetentionJob(
          snapshots,
          new RetentionProperties(PERIOD, 2),
          new TopologyMetrics(registry),
          Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void deletesExpiredSnapshotsInBatchesAndKeepsTheRest() {
    Instant cutoff = NOW.minus(PERIOD);
    snapshots.save(empty(cutoff.minus(Duration.ofDays(3))));
    snapshots.save(empty(cutoff.minus(Duration.ofDays(2))));
    snapshots.save(empty(cutoff.minusSeconds(1)));
    snapshots.save(empty(cutoff));
    snapshots.save(empty(NOW.minusSeconds(60)));

    job.run();

    assertThat(snapshots.all())
        .extracting(s -> s.window().end())
        .containsExactly(cutoff, NOW.minusSeconds(60));
    assertThat(registry.counter("architrace.snapshots.deleted").count()).isEqualTo(3.0);
  }

  @Test
  void propertiesMustBePositive() {
    assertThatThrownBy(() -> new RetentionProperties(Duration.ZERO, 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new RetentionProperties(PERIOD, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static Snapshot empty(Instant windowEnd) {
    return snapshot(AGENT, windowEnd, List.of(), List.of());
  }
}
