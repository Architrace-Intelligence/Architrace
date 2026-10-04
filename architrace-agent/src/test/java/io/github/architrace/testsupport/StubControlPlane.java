/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.testsupport;

import io.github.architrace.grpc.ControlPlaneClient;
import io.github.architrace.grpc.TransportClient;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent.PayloadCase;
import io.github.architrace.grpc.proto.ConfigUpdate;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.grpc.proto.ControlPlaneServiceGrpc;
import io.github.architrace.grpc.proto.GraphSnapshot;
import io.github.architrace.grpc.proto.SnapshotAck;
import io.github.architrace.grpc.proto.SnapshotRejected;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class StubControlPlane extends ControlPlaneServiceGrpc.ControlPlaneServiceImplBase
        implements AutoCloseable {

    private final String name = InProcessServerBuilder.generateName();
    private final Duration heartbeatInterval;
    private final List<AgentRegisterRequestedEvent> received = new CopyOnWriteArrayList<>();
    private final AtomicReference<StreamObserver<ControlPlaneCommand>> responses = new AtomicReference<>();
    private final AtomicBoolean rejectSnapshots = new AtomicBoolean();
    private final AtomicLong snapshotIds = new AtomicLong();
    private final Server server;

    private StubControlPlane(Duration heartbeatInterval) throws IOException {
        this.heartbeatInterval = heartbeatInterval;
        this.server = InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(this)
                .build()
                .start();
    }

    public static StubControlPlane start(Duration heartbeatInterval) throws IOException {
        return new StubControlPlane(heartbeatInterval);
    }

    public TransportClient transport() {
        return new ControlPlaneClient(
                InProcessChannelBuilder.forName(name).directExecutor().build());
    }

    public List<AgentRegisterRequestedEvent> received() {
        return List.copyOf(received);
    }

    public long count(PayloadCase payload) {
        return received.stream()
                .filter(event -> event.getPayloadCase() == payload)
                .count();
    }

    public List<GraphSnapshot> snapshots() {
        return received.stream()
                .filter(AgentRegisterRequestedEvent::hasSnapshot)
                .map(AgentRegisterRequestedEvent::getSnapshot)
                .toList();
    }

    public void rejectSnapshots(boolean reject) {
        rejectSnapshots.set(reject);
    }

    public void completeStream() {
        responses.get().onCompleted();
    }

    public void failStream() {
        responses
                .get()
                .onError(Status.UNAVAILABLE
                        .withDescription("control plane restarting")
                        .asRuntimeException());
    }

    @Override
    public StreamObserver<AgentRegisterRequestedEvent> connect(StreamObserver<ControlPlaneCommand> responseObserver) {
        responses.set(responseObserver);
        return new StreamObserver<>() {
            @Override
            public void onNext(AgentRegisterRequestedEvent event) {
                received.add(event);
                switch (event.getPayloadCase()) {
                    case REGISTER -> responseObserver.onNext(configUpdate());
                    case SNAPSHOT -> responseObserver.onNext(answer(event.getSnapshot()));
                    case HEARTBEAT, PAYLOAD_NOT_SET ->
                        responseObserver.onNext(ControlPlaneCommand.getDefaultInstance());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                received.add(AgentRegisterRequestedEvent.getDefaultInstance());
            }

            @Override
            public void onCompleted() {
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void close() {
        server.shutdownNow();
    }

    private ControlPlaneCommand configUpdate() {
        return ControlPlaneCommand.newBuilder()
                .setConfigUpdate(ConfigUpdate.newBuilder()
                        .setVersion("1")
                        .putConfig("heartbeat.interval-seconds", Long.toString(heartbeatInterval.toSeconds()))
                        .putConfig("snapshot.interval-seconds", "60"))
                .build();
    }

    private ControlPlaneCommand answer(GraphSnapshot snapshot) {
        if (rejectSnapshots.get()) {
            return ControlPlaneCommand.newBuilder()
                    .setSnapshotRejected(SnapshotRejected.newBuilder()
                            .setWindowEndEpochMs(snapshot.getWindowEndEpochMs())
                            .setReason("rejected by the stub"))
                    .build();
        }
        return ControlPlaneCommand.newBuilder()
                .setSnapshotAck(SnapshotAck.newBuilder()
                        .setWindowEndEpochMs(snapshot.getWindowEndEpochMs())
                        .setSnapshotId(snapshotIds.incrementAndGet()))
                .build();
    }
}
