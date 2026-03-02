/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.grpc;

import com.google.inject.Inject;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.grpc.proto.ControlPlaneServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.TimeUnit;

public class ControlPlaneClient implements TransportClient {

  private final ManagedChannel channel;
  private final ControlPlaneServiceGrpc.ControlPlaneServiceStub stub;

  @Inject
  public ControlPlaneClient(ManagedChannel channel) {
    this.channel = channel;
    this.stub = ControlPlaneServiceGrpc.newStub(channel);
  }

  @Override
  public StreamObserver<AgentRegisterRequestedEvent> open(
      StreamObserver<ControlPlaneCommand> inboundObserver) {
    return stub.connect(inboundObserver);
  }

  @Override
  public void close() {
    channel.shutdownNow();
    try {
      channel.awaitTermination(2, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}

