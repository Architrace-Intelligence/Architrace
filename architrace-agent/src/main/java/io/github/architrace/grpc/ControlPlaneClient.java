/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.grpc;

import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.grpc.proto.ControlPlaneServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class ControlPlaneClient implements TransportClient {

    private final ManagedChannel channel;

    public ControlPlaneClient(ManagedChannel channel) {
        this.channel = Objects.requireNonNull(channel, "channel");
    }

    @Override
    public StreamObserver<AgentRegisterRequestedEvent> open(StreamObserver<ControlPlaneCommand> inbound) {
        return ControlPlaneServiceGrpc.newStub(channel).connect(inbound);
    }

    @Override
    public void close() {
        channel.shutdownNow();
        try {
            channel.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }
}
