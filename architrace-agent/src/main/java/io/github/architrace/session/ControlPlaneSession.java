/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.session;

import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.inbound.InboundDispatcher;
import io.github.architrace.outbound.OutboundDispatcher;
import java.util.Objects;
import java.util.function.Consumer;

public class ControlPlaneSession {

  private final InboundDispatcher inbound;
  private final OutboundDispatcher outbound;
  private final Runnable completedCallback;
  private final Consumer<Throwable> errorCallback;

  public ControlPlaneSession(InboundDispatcher inbound,
                             OutboundDispatcher outbound,
                             Runnable completedCallback,
                             Consumer<Throwable> errorCallback) {
    this.inbound = Objects.requireNonNull(inbound);
    this.outbound = Objects.requireNonNull(outbound);
    this.completedCallback = Objects.requireNonNull(completedCallback, "completedCallback");
    this.errorCallback = Objects.requireNonNull(errorCallback, "errorCallback");
  }


  public void handleInbound(ControlPlaneCommand command) {
    inbound.dispatch(command, this);
  }

  public void send(AgentRegisterRequestedEvent event) {
    outbound.enqueue(event);
  }

  public AgentRegisterRequestedEvent takeOutbound() throws InterruptedException {
    return outbound.take();
  }

  public AgentRegisterRequestedEvent pollOutbound(long timeoutMs) throws InterruptedException {
    return outbound.poll(timeoutMs);
  }

  public void handleComplete() {
    completedCallback.run();
  }

  public void handleError(Throwable throwable) {
    errorCallback.accept(throwable);
  }
}
