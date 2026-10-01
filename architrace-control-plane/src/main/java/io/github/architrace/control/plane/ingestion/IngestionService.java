/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.ingestion;

import io.github.architrace.control.plane.topology.Agent;
import io.github.architrace.control.plane.topology.AgentId;
import io.github.architrace.control.plane.topology.AgentRegistration;
import io.github.architrace.control.plane.topology.AgentStore;
import io.github.architrace.control.plane.topology.Snapshot;
import io.github.architrace.control.plane.topology.SnapshotId;
import io.github.architrace.control.plane.topology.SnapshotStore;
import io.github.architrace.grpc.proto.GraphSnapshot;
import java.time.Clock;
import org.springframework.stereotype.Service;

@Service
public class IngestionService {

  private final AgentStore agents;
  private final SnapshotStore snapshots;
  private final IngestionMetrics metrics;
  private final Clock clock;
  private final SnapshotMapper mapper = new SnapshotMapper();

  public IngestionService(
      AgentStore agents, SnapshotStore snapshots, IngestionMetrics metrics, Clock clock) {
    this.agents = agents;
    this.snapshots = snapshots;
    this.metrics = metrics;
    this.clock = clock;
  }

  public Agent register(AgentRegistration registration) {
    return agents.register(registration, clock.instant());
  }

  public SnapshotId ingest(Agent agent, GraphSnapshot proto) {
    Snapshot snapshot;
    try {
      snapshot = mapper.toSnapshot(agent, proto, clock.instant());
    } catch (InvalidSnapshotException e) {
      metrics.snapshotRejected();
      throw e;
    }
    SnapshotId id = snapshots.save(snapshot);
    agents.touch(agent.id(), snapshot.receivedAt());
    metrics.snapshotIngested();
    return id;
  }

  public void heartbeat(AgentId agentId) {
    agents.touch(agentId, clock.instant());
  }
}
