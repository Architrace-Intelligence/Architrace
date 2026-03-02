/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.controlplane;

import com.google.inject.Singleton;
import io.github.architrace.grpc.proto.AgentRegister;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.grpc.stub.StreamObserver;
import java.util.Objects;

@Singleton
public class RegistrationService {

  public void sendRegister(String agentName, StreamObserver<AgentRegisterRequestedEvent> observer) {
    Objects.requireNonNull(observer, "observer");
    observer.onNext(
        AgentRegisterRequestedEvent.newBuilder()
            .setRegister(AgentRegister.newBuilder().setAgentName(agentName).build())
            .build());
  }
}
