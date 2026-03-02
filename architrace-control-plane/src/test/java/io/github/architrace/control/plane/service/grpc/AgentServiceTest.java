/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.service.grpc;

import io.github.architrace.grpc.proto.AgentHealthRequest;
import io.github.architrace.grpc.proto.AgentHealthResponse;
import io.github.architrace.grpc.proto.AgentRegister;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentServiceTest {

  @Test
  void connectShouldSendConfigUpdateForRegisterAndComplete() {
    AgentService sut = new AgentService();
    RecordingObserver<ControlPlaneCommand> responseObserver = new RecordingObserver<>();
    StreamObserver<AgentRegisterRequestedEvent> requestObserver = sut.connect(responseObserver);

    requestObserver.onNext(
        AgentRegisterRequestedEvent.newBuilder()
            .setRegister(AgentRegister.newBuilder().setAgentName("agent-a").build())
            .build());
    requestObserver.onCompleted();

    assertThat(responseObserver.values).hasSize(1);
    assertThat(responseObserver.values.getFirst().hasConfigUpdate()).isTrue();
    assertThat(responseObserver.completed).isTrue();
  }

  @Test
  void getAgentHealthShouldReturnLiveStatus() {
    AgentService sut = new AgentService();
    RecordingObserver<AgentHealthResponse> responseObserver = new RecordingObserver<>();

    sut.getAgentHealth(
        AgentHealthRequest.newBuilder().setAgentName("agent-a").build(),
        responseObserver);

    assertThat(responseObserver.values).hasSize(1);
    assertThat(responseObserver.values.getFirst().getLive()).isTrue();
    assertThat(responseObserver.values.getFirst().getLastSeenEpochMs()).isPositive();
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
      // No-op for test observer.
    }

    @Override
    public void onCompleted() {
      this.completed = true;
    }
  }
}
