/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.github.architrace.controlplane.AgentIdentity;
import io.github.architrace.controlplane.ControlPlaneBootstrapService;
import io.github.architrace.controlplane.ControlPlaneLifecycle;
import io.github.architrace.controlplane.RegistrationService;
import io.github.architrace.core.config.AgentConfig;
import io.github.architrace.grpc.TransportClient;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.otlp.GraphSnapshot;
import io.github.architrace.service.runtime.AgentRuntimeService;
import io.github.architrace.testsupport.TestDataProvider;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AgentRuntimeServiceTest {

    private static final Duration STARTUP_TIMEOUT = Duration.ofSeconds(10);

    @Test
    void runServesOtlpClosesAFailedControlPlaneSessionAndStopsWhenInterrupted() throws Exception {
        int otlpPort = TestDataProvider.findFreePort();
        AgentConfig config = TestDataProvider.agentConfig("localhost:1", otlpPort, Duration.ofMillis(10));
        AtomicBoolean lifecycleClosed = new AtomicBoolean();
        AgentRuntimeService sut = new AgentRuntimeService(failingBootstrap(config, lifecycleClosed));
        AtomicReference<Throwable> outcome = new AtomicReference<>();
        Thread agent = Thread.ofPlatform().start(() -> {
            try {
                sut.run(config);
            } catch (Throwable throwable) {
                outcome.set(throwable);
            }
        });

        await().atMost(STARTUP_TIMEOUT).until(() -> isListening(otlpPort));
        await().atMost(STARTUP_TIMEOUT).untilTrue(lifecycleClosed);
        agent.interrupt();
        agent.join(STARTUP_TIMEOUT.toMillis());

        assertThat(agent.isAlive()).isFalse();
        assertThat(outcome.get()).isInstanceOf(InterruptedException.class);
        assertThat(isListening(otlpPort)).isFalse();
    }

    private static ControlPlaneBootstrapService failingBootstrap(AgentConfig config, AtomicBoolean closed) {
        return new ControlPlaneBootstrapService(null) {
            @Override
            public ControlPlaneLifecycle bootstrap(AgentConfig ignored) {
                return new ControlPlaneLifecycle(
                        AgentIdentity.from(config),
                        new ThrowingTransportClient(closed),
                        new RegistrationService(),
                        List.of());
            }
        };
    }

    private static boolean isListening(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 200);
            return true;
        } catch (IOException _) {
            return false;
        }
    }

    private static final class ThrowingTransportClient implements TransportClient {

        private final AtomicBoolean closeCalled;

        private ThrowingTransportClient(AtomicBoolean closeCalled) {
            this.closeCalled = closeCalled;
        }

        @Override
        public StreamObserver<AgentRegisterRequestedEvent> open(StreamObserver<ControlPlaneCommand> inboundObserver) {
            throw new IllegalStateException("control plane unreachable");
        }

        @Override
        public void close() {
            closeCalled.set(true);
        }

        @Override
        public void send(GraphSnapshot snapshot) {
            throw new UnsupportedOperationException("no session was ever opened");
        }
    }
}
