/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.grpc;


import io.github.architrace.controlplane.ControlPlaneRegistry;
import io.github.architrace.grpc.proto.AgentHealthRequest;
import io.github.architrace.grpc.proto.AgentHealthResponse;
import io.github.architrace.grpc.proto.AgentRegister;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ControlPlaneServiceImplTest {

  private static final String AGENT_A = "agent-a";
  private static final String AGENT_B = "agent-b";
  private static final String UNKNOWN_AGENT = "agent-x";
  private static final long HEALTH_LIVE_THRESHOLD_MS = 15_000L;

  private ControlPlaneRegistry registry;
  private ControlPlaneServiceImpl sut;

  @BeforeEach
  void setUp() {
    registry = new ControlPlaneRegistry();
    sut = new ControlPlaneServiceImpl(registry);
  }

  @AfterEach
  void tearDown() {
    sut.close();
  }

  @Test
  void connectShouldRegisterAndComplete() {
    RecordingObserver<ControlPlaneCommand> responseObserver = new RecordingObserver<>();
    StreamObserver<AgentRegisterRequestedEvent> requestObserver = sut.connect(responseObserver);

    requestObserver.onNext(
        AgentRegisterRequestedEvent.newBuilder()
            .setRegister(AgentRegister.newBuilder().setAgentName(AGENT_A).build())
            .build());

    assertThat(responseObserver.values.stream().anyMatch(ControlPlaneCommand::hasConfigUpdate)).isTrue();

    ControlPlaneRegistry.HealthState health = registry.health(AGENT_A, HEALTH_LIVE_THRESHOLD_MS);
    assertThat(health.live()).isTrue();
    assertThat(health.lastSeenEpochMs()).isPositive();

    requestObserver.onCompleted();
    assertThat(responseObserver.completed).isTrue();
    assertThat(registry.health(AGENT_A, HEALTH_LIVE_THRESHOLD_MS).live()).isFalse();
  }

  @Test
  void connectShouldUnregisterOnError() {
    RecordingObserver<ControlPlaneCommand> responseObserver = new RecordingObserver<>();
    StreamObserver<AgentRegisterRequestedEvent> requestObserver = sut.connect(responseObserver);

    requestObserver.onNext(
        AgentRegisterRequestedEvent.newBuilder()
            .setRegister(AgentRegister.newBuilder().setAgentName(AGENT_B).build())
            .build());
    requestObserver.onError(new RuntimeException("boom"));

    assertThat(registry.health(AGENT_B, HEALTH_LIVE_THRESHOLD_MS).live()).isFalse();
  }

  @Test
  void connectShouldIgnoreEventsWithoutRegister() {
    RecordingObserver<ControlPlaneCommand> responseObserver = new RecordingObserver<>();
    StreamObserver<AgentRegisterRequestedEvent> requestObserver = sut.connect(responseObserver);

    requestObserver.onNext(AgentRegisterRequestedEvent.getDefaultInstance());

    assertThat(registry.health(UNKNOWN_AGENT, HEALTH_LIVE_THRESHOLD_MS).live()).isFalse();
  }

  @Test
  void getAgentHealthShouldReturnRegistryHealth() {
    RecordingObserver<ControlPlaneCommand> connectObserver = new RecordingObserver<>();
    sut.connect(connectObserver).onNext(
        AgentRegisterRequestedEvent.newBuilder()
            .setRegister(AgentRegister.newBuilder().setAgentName(AGENT_A).build())
            .build());
    RecordingObserver<AgentHealthResponse> responseObserver = new RecordingObserver<>();
    AgentHealthRequest request = AgentHealthRequest.newBuilder().setAgentName(AGENT_A).build();

    sut.getAgentHealth(request, responseObserver);

    assertThat(responseObserver.values).hasSize(1);
    AgentHealthResponse response = responseObserver.values.get(0);
    assertThat(response.getLive()).isTrue();
    assertThat(response.getLastSeenEpochMs()).isPositive();
    assertThat(responseObserver.completed).isTrue();
  }

  private static final class RecordingObserver<T> implements StreamObserver<T> {
    private final List<T> values = new ArrayList<>();
    private boolean completed;

    @Override
    public void onNext(T value) {
      values.add(value);
    }

    @Override
    public void onError(Throwable throwable) {
      // No-op for tests; failure path is asserted through registry state.
    }

    @Override
    public void onCompleted() {
      this.completed = true;
    }
  }
}
