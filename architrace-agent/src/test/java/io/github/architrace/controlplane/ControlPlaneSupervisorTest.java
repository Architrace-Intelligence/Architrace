/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.controlplane;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.github.architrace.grpc.TransportClient;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent.PayloadCase;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.publish.PublisherStats;
import io.github.architrace.publish.SnapshotQueue;
import io.github.architrace.testsupport.StubControlPlane;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.time.Duration;
import java.time.InstantSource;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ControlPlaneSupervisorTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final AgentIdentity IDENTITY = new AgentIdentity("agent-a", "1.0", "demo", "DEV", "cluster-1");

    private final PublisherStats stats = new PublisherStats();
    private final SnapshotQueue snapshots = new SnapshotQueue(4);
    private Thread runner;

    @AfterEach
    void stop() throws InterruptedException {
        runner.interrupt();
        runner.join(TIMEOUT.toMillis());
        assertThat(runner.isAlive()).isFalse();
    }

    @Test
    void reconnectsAfterEveryFailedSessionUntilInterrupted() {
        AtomicInteger openAttempts = new AtomicInteger();
        ControlPlaneSupervisor sut = new ControlPlaneSupervisor(
                () -> session(new UnreachableTransport(openAttempts)), Duration.ofMillis(10), stats);

        runner = Thread.ofPlatform().start(() -> run(sut));

        await().atMost(TIMEOUT).until(() -> stats.endedSessions() >= 3);
        assertThat(openAttempts.get()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void reconnectsAfterTheServerCompletesTheStream() throws IOException {
        try (StubControlPlane controlPlane = StubControlPlane.start(Duration.ofSeconds(30))) {
            ControlPlaneSupervisor sut =
                    new ControlPlaneSupervisor(() -> session(controlPlane.transport()), Duration.ofMillis(10), stats);
            runner = Thread.ofPlatform().start(() -> run(sut));
            await().atMost(TIMEOUT).until(() -> controlPlane.count(PayloadCase.REGISTER) == 1);

            controlPlane.completeStream();

            await().atMost(TIMEOUT).until(() -> controlPlane.count(PayloadCase.REGISTER) >= 2);
            assertThat(stats.endedSessions()).isGreaterThanOrEqualTo(1);
        }
    }

    private ControlPlaneSession session(TransportClient transport) {
        return new ControlPlaneSession(IDENTITY, transport, snapshots, stats, InstantSource.system());
    }

    private static void run(ControlPlaneSupervisor supervisor) {
        try {
            supervisor.run();
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    private record UnreachableTransport(AtomicInteger openAttempts) implements TransportClient {

        @Override
        public StreamObserver<AgentRegisterRequestedEvent> open(StreamObserver<ControlPlaneCommand> inbound) {
            openAttempts.incrementAndGet();
            throw Status.UNAVAILABLE.withDescription("connection refused").asRuntimeException();
        }

        @Override
        public void close() {
            openAttempts.get();
        }
    }
}
