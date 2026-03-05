/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.controlplane;

import io.github.architrace.grpc.TransportClient;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.grpc.proto.GraphBatch;
import io.github.architrace.otlp.GraphSnapshot;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ControlPlaneLifecycleTest {

  @Test
  void runShouldRegisterDrainOutboundAndCloseTransport() {
    RecordingTransportClient transportClient = new RecordingTransportClient();
    RegistrationService registrationService = new CompletingRegistrationService(transportClient, 75);
    ControlPlaneLifecycle sut =
        new ControlPlaneLifecycle("agent-a", transportClient, registrationService, List.of());

    sut.publishGraphBatch(GraphBatch.newBuilder().setAgentName("agent-a").setObservedAtEpochMs(1L).build());
    sut.run();

    assertThat(transportClient.closed.get()).isTrue();
    assertThat(transportClient.outboundEvents.stream().anyMatch(AgentRegisterRequestedEvent::hasRegister)).isTrue();
    assertThat(transportClient.outboundEvents.stream().anyMatch(AgentRegisterRequestedEvent::hasGraphBatch)).isTrue();
  }

  @Test
  void closeShouldReleaseAwait() {
    RecordingTransportClient transportClient = new RecordingTransportClient();
    ControlPlaneLifecycle sut =
        new ControlPlaneLifecycle("agent-a", transportClient, new RegistrationService(), List.of());

    sut.close();
    sut.await();

    assertThat(transportClient.closed.get()).isTrue();
  }

  private static final class CompletingRegistrationService extends RegistrationService {
    private final RecordingTransportClient transportClient;
    private final long delayMs;

    private CompletingRegistrationService(RecordingTransportClient transportClient, long delayMs) {
      this.transportClient = transportClient;
      this.delayMs = delayMs;
    }

    @Override
    public void sendRegister(String agentName, StreamObserver<AgentRegisterRequestedEvent> observer) {
      super.sendRegister(agentName, observer);
      Thread.ofVirtual().start(() -> {
        try {
          Thread.sleep(delayMs);
        } catch (InterruptedException _) {
          Thread.currentThread().interrupt();
        }
        StreamObserver<ControlPlaneCommand> inbound = transportClient.inboundObserver.get();
        if (inbound != null) {
          inbound.onCompleted();
        }
      });
    }
  }

  private static final class RecordingTransportClient implements TransportClient {
    private final AtomicReference<StreamObserver<ControlPlaneCommand>> inboundObserver = new AtomicReference<>();
    private final List<AgentRegisterRequestedEvent> outboundEvents = new ArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    @Override
    public StreamObserver<AgentRegisterRequestedEvent> open(
        StreamObserver<ControlPlaneCommand> inboundObserver) {
      this.inboundObserver.set(inboundObserver);
      return new StreamObserver<>() {
        @Override
        public void onNext(AgentRegisterRequestedEvent value) {
          outboundEvents.add(value);
        }

        @Override
        public void onError(Throwable throwable) {
          // No-op for test observer.
        }

        @Override
        public void onCompleted() {
          // No-op for test observer.
        }
      };
    }

    @Override
    public void close() {
      closed.set(true);
    }

    @Override
    public void send(GraphSnapshot snapshot) {

    }
  }
}
