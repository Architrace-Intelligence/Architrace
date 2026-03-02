/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.inbound;

import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ControlPlaneInboundObserver implements StreamObserver<ControlPlaneCommand> {

  private static final Logger log = LoggerFactory.getLogger(ControlPlaneInboundObserver.class);

  private final String agentName;
  private final AtomicReference<StreamObserver<AgentRegisterRequestedEvent>> agentObserver;

  public ControlPlaneInboundObserver(String agentName,
                                     AtomicReference<StreamObserver<AgentRegisterRequestedEvent>> agentObserver) {
    this.agentName = agentName;
    this.agentObserver = agentObserver;
  }

  @Override
  public void onNext(ControlPlaneCommand command) {
    log.info("Received control plane event {}", command);
    if (command.hasConfigUpdate()) {
      log.info("Received control plane event {}", command);
    }
  }

  @Override
  public void onError(Throwable throwable) {
    agentObserver.get().onError(throwable);
    log.error("Control-plane stream failed for agent '{}'", agentName, throwable);
//    shutdownSignal.countDown();
  }

  @Override
  public void onCompleted() {
    log.info("Control-plane stream completed for agent '{}'", agentName);
//    shutdownSignal.countDown();
    agentObserver.get().onCompleted();
  }
}