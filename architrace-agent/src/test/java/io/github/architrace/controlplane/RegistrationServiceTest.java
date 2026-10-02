/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.controlplane;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RegistrationServiceTest {

  @Test
  void sendRegisterShouldSendRegisterEvent() {
    RegistrationService sut = new RegistrationService();
    RecordingObserver observer = new RecordingObserver();

    sut.sendRegister(new AgentIdentity("agent-a", "0.1.0", "demo", "DEV", "cluster-1"), observer);

    assertThat(observer.values).hasSize(1);
    assertThat(observer.values.getFirst().hasRegister()).isTrue();
    var register = observer.values.getFirst().getRegister();
    assertThat(register.getAgentName()).isEqualTo("agent-a");
    assertThat(register.getAgentVersion()).isEqualTo("0.1.0");
    assertThat(register.getProject()).isEqualTo("demo");
    assertThat(register.getEnvironment()).isEqualTo("DEV");
    assertThat(register.getClusterId()).isEqualTo("cluster-1");
  }

  private static final class RecordingObserver implements StreamObserver<AgentRegisterRequestedEvent> {
    private final List<AgentRegisterRequestedEvent> values = new ArrayList<>();

    @Override
    public void onNext(AgentRegisterRequestedEvent value) {
      values.add(value);
    }

    @Override
    public void onError(Throwable throwable) {
    }

    @Override
    public void onCompleted() {
    }
  }
}
