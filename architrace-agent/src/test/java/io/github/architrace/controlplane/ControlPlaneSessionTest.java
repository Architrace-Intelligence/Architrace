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
import io.github.architrace.testsupport.TestSnapshots;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.time.InstantSource;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ControlPlaneSessionTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final AgentIdentity IDENTITY = new AgentIdentity("agent-a", "1.0", "demo", "DEV", "cluster-1");

    private final SnapshotQueue snapshots = new SnapshotQueue(8);
    private final PublisherStats stats = new PublisherStats();
    private final AtomicReference<Throwable> outcome = new AtomicReference<>();
    private StubControlPlane controlPlane;
    private Thread runner;

    @AfterEach
    void stop() throws InterruptedException {
        if (runner != null) {
            runner.interrupt();
            runner.join(TIMEOUT.toMillis());
        }
        if (controlPlane != null) {
            controlPlane.close();
        }
    }

    @Test
    void registersPublishesQueuedSnapshotsHeartbeatsAndEndsWhenTheServerCompletes() throws Exception {
        controlPlane = StubControlPlane.start(Duration.ofSeconds(1));
        snapshots.offer(TestSnapshots.full());
        ControlPlaneSession sut = session(controlPlane.transport());

        start(sut);

        await().atMost(TIMEOUT).until(() -> stats.acknowledgedCount() == 1);
        await().atMost(TIMEOUT).until(() -> controlPlane.count(PayloadCase.HEARTBEAT) >= 1);
        assertThat(controlPlane.received().getFirst().hasRegister()).isTrue();
        assertThat(controlPlane.received().getFirst().getRegister().getAgentName())
                .isEqualTo("agent-a");
        assertThat(controlPlane.received().getFirst().getRegister().getClusterId())
                .isEqualTo("cluster-1");
        assertThat(controlPlane.snapshots())
                .singleElement()
                .satisfies(snapshot -> assertThat(snapshot.getEdgesCount()).isEqualTo(5));
        assertThat(sut.heartbeatInterval()).isEqualTo(Duration.ofSeconds(1));
        assertThat(stats.publishedCount()).isEqualTo(1);
        assertThat(snapshots.size()).isZero();

        controlPlane.completeStream();
        runner.join(TIMEOUT.toMillis());

        assertThat(runner.isAlive()).isFalse();
        assertThat(outcome.get()).isNull();
    }

    @Test
    void rejectedSnapshotsAreCountedAndTheSessionGoesOn() throws Exception {
        controlPlane = StubControlPlane.start(Duration.ofSeconds(30));
        controlPlane.rejectSnapshots(true);
        start(session(controlPlane.transport()));

        snapshots.offer(TestSnapshots.full());

        await().atMost(TIMEOUT).until(() -> stats.rejectedCount() == 1);
        assertThat(stats.acknowledgedCount()).isZero();
        assertThat(runner.isAlive()).isTrue();
    }

    @Test
    void failedStreamEndsTheSessionWithTheError() throws Exception {
        controlPlane = StubControlPlane.start(Duration.ofSeconds(30));
        start(session(controlPlane.transport()));
        await().atMost(TIMEOUT).until(() -> controlPlane.count(PayloadCase.REGISTER) == 1);

        controlPlane.failStream();
        runner.join(TIMEOUT.toMillis());

        assertThat(runner.isAlive()).isFalse();
        assertThat(outcome.get()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void unsentSnapshotGoesBackToTheFrontOfTheQueue() throws Exception {
        snapshots.offer(TestSnapshots.full());
        start(session(new BrokenTransport()));

        runner.join(TIMEOUT.toMillis());

        assertThat(runner.isAlive()).isFalse();
        assertThat(outcome.get()).isInstanceOf(RuntimeException.class);
        assertThat(snapshots.size()).isEqualTo(1);
        assertThat(stats.publishedCount()).isZero();
    }

    private ControlPlaneSession session(TransportClient transport) {
        return new ControlPlaneSession(IDENTITY, transport, snapshots, stats, InstantSource.system());
    }

    private void start(ControlPlaneSession session) {
        runner = Thread.ofPlatform().start(() -> {
            try {
                session.run();
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            } catch (RuntimeException e) {
                outcome.set(e);
            }
        });
    }

    private static final class BrokenTransport implements TransportClient {

        @Override
        public StreamObserver<AgentRegisterRequestedEvent> open(StreamObserver<ControlPlaneCommand> inbound) {
            return new StreamObserver<>() {
                @Override
                public void onNext(AgentRegisterRequestedEvent event) {
                    if (event.hasSnapshot()) {
                        throw new IllegalStateException("stream is gone");
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    inbound.onError(throwable);
                }

                @Override
                public void onCompleted() {
                    inbound.onCompleted();
                }
            };
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException("closed once by the session; nothing to release");
        }
    }
}
