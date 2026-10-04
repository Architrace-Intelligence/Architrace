/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.controlplane;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.graph.GraphSnapshot;
import io.github.architrace.grpc.TransportClient;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.grpc.proto.GraphBatch;
import io.github.architrace.testsupport.RecordingObserver;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ControlPlaneLifecycleTest {

    private static final AgentIdentity IDENTITY = new AgentIdentity("agent-a", "0.1.0", "demo", "DEV", "cluster-1");

    @Test
    void runShouldRegisterDrainOutboundAndCloseTransport() {
        RecordingTransportClient transportClient = new RecordingTransportClient();
        RegistrationService registrationService = new CompletingRegistrationService(transportClient, 75);
        ControlPlaneLifecycle sut =
                new ControlPlaneLifecycle(IDENTITY, transportClient, registrationService, List.of());

        sut.publishGraphBatch(GraphBatch.newBuilder()
                .setAgentName("agent-a")
                .setObservedAtEpochMs(1L)
                .build());
        sut.run();

        assertThat(transportClient.closed.get()).isTrue();
        List<AgentRegisterRequestedEvent> outbound = transportClient.outbound.values();
        assertThat(outbound.stream().anyMatch(AgentRegisterRequestedEvent::hasRegister))
                .isTrue();
        assertThat(outbound.stream().anyMatch(AgentRegisterRequestedEvent::hasGraphBatch))
                .isTrue();
    }

    @Test
    void closeShouldReleaseAwait() {
        RecordingTransportClient transportClient = new RecordingTransportClient();
        ControlPlaneLifecycle sut =
                new ControlPlaneLifecycle(IDENTITY, transportClient, new RegistrationService(), List.of());

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
        public void sendRegister(AgentIdentity identity, StreamObserver<AgentRegisterRequestedEvent> observer) {
            super.sendRegister(identity, observer);
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
        private final RecordingObserver<AgentRegisterRequestedEvent> outbound = new RecordingObserver<>();
        private final AtomicBoolean closed = new AtomicBoolean(false);

        @Override
        public StreamObserver<AgentRegisterRequestedEvent> open(StreamObserver<ControlPlaneCommand> inboundObserver) {
            this.inboundObserver.set(inboundObserver);
            return outbound;
        }

        @Override
        public void close() {
            closed.set(true);
        }

        @Override
        public void send(GraphSnapshot snapshot) {}
    }
}
