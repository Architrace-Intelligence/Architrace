/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.controlplane;

import io.github.architrace.grpc.proto.ConfigUpdate;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.grpc.stub.StreamObserver;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class ControlPlaneRegistry {

  private final ConcurrentHashMap<String, AgentSession> sessions = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AtomicLong> versionSequence = new ConcurrentHashMap<>();

  public void register(String agentName, StreamObserver<ControlPlaneCommand> responseObserver) {
    sessions.put(
        agentName,
        new AgentSession(
            responseObserver,
            System.currentTimeMillis()));

    sendConfig(
        ConfigUpdate.newBuilder()
            .setVersion(nextVersion(agentName))
            .putConfig("agent.mode", "managed")
            .putConfig("agent.bootstrap", "done")
            .build(),
        responseObserver);
  }

  public void touch(String agentName) {
    AgentSession session = sessions.get(agentName);
    if (session != null) {
      session.lastSeenEpochMs = System.currentTimeMillis();
    }
  }

  public void unregister(String agentName) {
    if (agentName != null) {
      sessions.remove(agentName);
    }
  }

  public HealthState health(String agentName, long liveThresholdMs) {
    AgentSession session = sessions.get(agentName);
    if (session == null) {
      return new HealthState(false, 0L);
    }

    long lastSeen = session.lastSeenEpochMs;
    boolean live = (System.currentTimeMillis() - lastSeen) <= liveThresholdMs;
    return new HealthState(live, lastSeen);
  }

  public void tick() {
    long now = System.currentTimeMillis();
    for (Map.Entry<String, AgentSession> entry : sessions.entrySet()) {
      String agentName = entry.getKey();
      AgentSession session = entry.getValue();
      sendConfig(
          ConfigUpdate.newBuilder()
              .setVersion(nextVersion(agentName))
              .putConfig("control.updatedAtEpochMs", Long.toString(now))
              .build(),
          session.responseObserver);
    }
  }

  private String nextVersion(String agentName) {
    AtomicLong seq = versionSequence.computeIfAbsent(agentName, ignored -> new AtomicLong(0));
    return Long.toString(seq.incrementAndGet());
  }

  private void sendConfig(
      ConfigUpdate update, StreamObserver<ControlPlaneCommand> responseObserver) {
    responseObserver.onNext(ControlPlaneCommand.newBuilder().setConfigUpdate(update).build());
  }

  public record HealthState(boolean live, long lastSeenEpochMs) {
  }

  private static final class AgentSession {
    private final StreamObserver<ControlPlaneCommand> responseObserver;
    private volatile long lastSeenEpochMs;

    private AgentSession(
        StreamObserver<ControlPlaneCommand> responseObserver,
        long lastSeenEpochMs) {
      this.responseObserver = responseObserver;
      this.lastSeenEpochMs = lastSeenEpochMs;
    }
  }
}
