/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.controlplane;

import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationServiceTest {

  @Test
  void sendRegisterShouldSendRegisterEvent() {
    RegistrationService sut = new RegistrationService();
    RecordingObserver observer = new RecordingObserver();

    sut.sendRegister("agent-a", observer);

    assertThat(observer.values).hasSize(1);
    assertThat(observer.values.getFirst().hasRegister()).isTrue();
    assertThat(observer.values.getFirst().getRegister().getAgentName()).isEqualTo("agent-a");
  }

  private static final class RecordingObserver implements StreamObserver<AgentRegisterRequestedEvent> {
    private final List<AgentRegisterRequestedEvent> values = new ArrayList<>();

    @Override
    public void onNext(AgentRegisterRequestedEvent value) {
      values.add(value);
    }

    @Override
    public void onError(Throwable throwable) {
      // No-op for test observer.
    }

    @Override
    public void onCompleted() {
      // No-op for test observer.
    }
  }
}
