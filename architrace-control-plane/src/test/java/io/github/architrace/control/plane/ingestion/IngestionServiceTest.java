/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.ingestion;

import static io.github.architrace.control.plane.ingestion.SnapshotProtos.WINDOW_END;
import static io.github.architrace.control.plane.ingestion.SnapshotProtos.ordersSnapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.architrace.control.plane.topology.Agent;
import io.github.architrace.control.plane.topology.AgentRegistration;
import io.github.architrace.control.plane.topology.InMemoryAgentStore;
import io.github.architrace.control.plane.topology.InMemorySnapshotStore;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.SnapshotId;
import io.github.architrace.grpc.proto.GraphSnapshot;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class IngestionServiceTest {

  private static final Instant NOW = WINDOW_END.plusSeconds(5);
  private static final AgentRegistration REGISTRATION =
      new AgentRegistration("prod-eu1-a", "0.4.0", new Scope("webshop", "PROD", "k8s-prod-eu1"));

  private final InMemoryAgentStore agents = new InMemoryAgentStore();
  private final InMemorySnapshotStore snapshots = new InMemorySnapshotStore();
  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
  private final IngestionService service =
      new IngestionService(
          agents,
          snapshots,
          new IngestionMetrics(registry),
          Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void registersWithTheClockTime() {
    Agent agent = service.register(REGISTRATION);

    assertThat(agent.firstSeenAt()).isEqualTo(NOW);
    assertThat(agents.find(agent.id())).isPresent();
  }

  @Test
  void ingestStoresTouchesAndCounts() {
    Agent agent = service.register(REGISTRATION);
    service.heartbeat(agent.id());

    SnapshotId id = service.ingest(agent, ordersSnapshot());

    assertThat(snapshots.find(id)).get().satisfies(s -> assertThat(s.nodes()).hasSize(3));
    assertThat(agents.find(agent.id())).get().satisfies(a -> assertThat(a.lastSeenAt()).isEqualTo(NOW));
    assertThat(registry.counter("architrace.snapshots.ingested").count()).isEqualTo(1.0);
    assertThat(registry.counter("architrace.snapshots.rejected").count()).isZero();
  }

  @Test
  void invalidSnapshotIsCountedAndNotStored() {
    Agent agent = service.register(REGISTRATION);
    GraphSnapshot invalid =
        GraphSnapshot.newBuilder().setWindowStartEpochMs(1).setWindowEndEpochMs(1).build();

    assertThatThrownBy(() -> service.ingest(agent, invalid))
        .isInstanceOf(InvalidSnapshotException.class);

    assertThat(snapshots.all()).isEmpty();
    assertThat(registry.counter("architrace.snapshots.rejected").count()).isEqualTo(1.0);
  }
}
