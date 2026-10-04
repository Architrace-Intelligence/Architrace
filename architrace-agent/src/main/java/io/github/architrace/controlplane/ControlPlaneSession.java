/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.controlplane;

import io.github.architrace.graph.GraphSnapshot;
import io.github.architrace.grpc.TransportClient;
import io.github.architrace.grpc.proto.AgentRegister;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.Heartbeat;
import io.github.architrace.publish.PublisherStats;
import io.github.architrace.publish.SnapshotProtoMapper;
import io.github.architrace.publish.SnapshotQueue;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.time.InstantSource;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ControlPlaneSession {

    static final Duration DEFAULT_HEARTBEAT_INTERVAL = Duration.ofSeconds(30);
    private static final Duration POLL_TIMEOUT = Duration.ofMillis(200);
    private static final Logger log = LoggerFactory.getLogger(ControlPlaneSession.class);

    private final AgentIdentity identity;
    private final TransportClient transport;
    private final SnapshotQueue snapshots;
    private final PublisherStats stats;
    private final InstantSource clock;
    private final AtomicReference<Duration> heartbeatInterval = new AtomicReference<>(DEFAULT_HEARTBEAT_INTERVAL);

    public ControlPlaneSession(
            AgentIdentity identity,
            TransportClient transport,
            SnapshotQueue snapshots,
            PublisherStats stats,
            InstantSource clock) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.snapshots = Objects.requireNonNull(snapshots, "snapshots");
        this.stats = Objects.requireNonNull(stats, "stats");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void run() throws InterruptedException {
        CompletableFuture<Void> closed = new CompletableFuture<>();
        try (transport;
                var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {
            StreamObserver<AgentRegisterRequestedEvent> requests =
                    transport.open(new SessionInbound(closed, heartbeatInterval, stats));
            requests.onNext(registration());
            stats.connected(true);
            log.info("Control plane session opened for agent '{}'", identity.name());
            scope.fork(() -> publish(requests, closed));
            scope.fork(() -> heartbeat(requests, closed));
            scope.fork(() -> awaitClosed(closed));
            scope.join();
        } finally {
            stats.connected(false);
        }
    }

    public Duration heartbeatInterval() {
        return heartbeatInterval.get();
    }

    private Void publish(StreamObserver<AgentRegisterRequestedEvent> requests, CompletableFuture<Void> closed)
            throws InterruptedException {
        while (!closed.isDone()) {
            GraphSnapshot snapshot = snapshots.poll(POLL_TIMEOUT);
            if (snapshot != null) {
                send(requests, snapshot);
            }
        }
        return null;
    }

    private void send(StreamObserver<AgentRegisterRequestedEvent> requests, GraphSnapshot snapshot) {
        try {
            requests.onNext(AgentRegisterRequestedEvent.newBuilder()
                    .setSnapshot(SnapshotProtoMapper.toProto(snapshot))
                    .build());
            stats.published();
        } catch (RuntimeException e) {
            snapshots.requeue(snapshot);
            throw e;
        }
    }

    private Void heartbeat(StreamObserver<AgentRegisterRequestedEvent> requests, CompletableFuture<Void> closed)
            throws InterruptedException, ExecutionException {
        while (!closed.isDone()) {
            try {
                closed.get(heartbeatInterval.get().toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException _) {
                requests.onNext(AgentRegisterRequestedEvent.newBuilder()
                        .setHeartbeat(Heartbeat.newBuilder()
                                .setSentAtEpochMs(clock.instant().toEpochMilli()))
                        .build());
            }
        }
        return null;
    }

    private static Void awaitClosed(CompletableFuture<Void> closed) throws InterruptedException, ExecutionException {
        closed.get();
        return null;
    }

    private AgentRegisterRequestedEvent registration() {
        return AgentRegisterRequestedEvent.newBuilder()
                .setRegister(AgentRegister.newBuilder()
                        .setAgentName(identity.name())
                        .setAgentVersion(identity.version())
                        .setProject(identity.project())
                        .setEnvironment(identity.environment())
                        .setClusterId(identity.cluster()))
                .build();
    }
}
