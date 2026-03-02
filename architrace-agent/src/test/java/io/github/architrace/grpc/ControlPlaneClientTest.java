/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.grpc;

import io.github.architrace.controlplane.ControlPlaneRegistry;
import io.github.architrace.grpc.proto.AgentRegister;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import io.github.architrace.testsupport.TestDataProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ControlPlaneClientTest {

  private Server server;
  private ControlPlaneServiceImpl service;

  @AfterEach
  void tearDown() throws Exception {
    if (service != null) {
      service.close();
    }
    if (server != null) {
      server.shutdownNow();
      server.awaitTermination(2, TimeUnit.SECONDS);
    }
  }

  @Test
  void openShouldConnectAndReceiveConfigUpdate() throws Exception {
    int port = TestDataProvider.findFreePort();
    service = new ControlPlaneServiceImpl(new ControlPlaneRegistry());
    server = ServerBuilder.forPort(port).addService(service).build().start();

    ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", port).usePlaintext().build();
    try (ControlPlaneClient sut = new ControlPlaneClient(channel)) {
      RecordingObserver inbound = new RecordingObserver();
      StreamObserver<AgentRegisterRequestedEvent> requestObserver = sut.open(inbound);

      requestObserver.onNext(
          AgentRegisterRequestedEvent.newBuilder()
              .setRegister(AgentRegister.newBuilder().setAgentName("agent-a").build())
              .build());

      waitUntil(() -> !inbound.values.isEmpty(), 2_000);
      assertThat(inbound.values.stream().anyMatch(ControlPlaneCommand::hasConfigUpdate)).isTrue();
    }
  }

  private static void waitUntil(BooleanSupplier condition, long timeoutMs) throws InterruptedException {
    long start = System.currentTimeMillis();
    while (!condition.getAsBoolean() && System.currentTimeMillis() - start < timeoutMs) {
      Thread.sleep(25);
    }
  }

  @FunctionalInterface
  private interface BooleanSupplier {
    boolean getAsBoolean();
  }

  private static final class RecordingObserver implements StreamObserver<ControlPlaneCommand> {
    private final List<ControlPlaneCommand> values = new ArrayList<>();

    @Override
    public void onNext(ControlPlaneCommand value) {
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
