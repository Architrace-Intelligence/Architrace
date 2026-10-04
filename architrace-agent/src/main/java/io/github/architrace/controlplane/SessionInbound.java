/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.controlplane;

import io.github.architrace.grpc.proto.ConfigUpdate;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.grpc.proto.SnapshotRejected;
import io.github.architrace.publish.PublisherStats;
import io.grpc.stub.StreamObserver;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SessionInbound implements StreamObserver<ControlPlaneCommand> {

    static final String HEARTBEAT_INTERVAL_KEY = "heartbeat.interval-seconds";

    private static final Logger log = LoggerFactory.getLogger(SessionInbound.class);

    private final CompletableFuture<Void> closed;
    private final AtomicReference<Duration> heartbeatInterval;
    private final PublisherStats stats;

    SessionInbound(CompletableFuture<Void> closed, AtomicReference<Duration> heartbeatInterval, PublisherStats stats) {
        this.closed = closed;
        this.heartbeatInterval = heartbeatInterval;
        this.stats = stats;
    }

    @Override
    public void onNext(ControlPlaneCommand command) {
        switch (command.getPayloadCase()) {
            case CONFIG_UPDATE -> apply(command.getConfigUpdate());
            case SNAPSHOT_ACK -> stats.acknowledged();
            case SNAPSHOT_REJECTED -> reject(command.getSnapshotRejected());
            case PAYLOAD_NOT_SET -> log.debug("Ignoring an empty control plane command");
        }
    }

    @Override
    public void onError(Throwable throwable) {
        closed.completeExceptionally(throwable);
    }

    @Override
    public void onCompleted() {
        closed.complete(null);
    }

    private void apply(ConfigUpdate update) {
        Optional.ofNullable(update.getConfigMap().get(HEARTBEAT_INTERVAL_KEY))
                .flatMap(SessionInbound::seconds)
                .ifPresent(heartbeatInterval::set);
        log.info("Control plane configuration version {} applied: {}", update.getVersion(), update.getConfigMap());
    }

    private void reject(SnapshotRejected rejected) {
        stats.rejected();
        log.warn(
                "Control plane rejected the snapshot ending at {}: {}",
                rejected.getWindowEndEpochMs(),
                rejected.getReason());
    }

    private static Optional<Duration> seconds(String value) {
        try {
            long seconds = Long.parseLong(value.trim());
            return seconds > 0 ? Optional.of(Duration.ofSeconds(seconds)) : Optional.empty();
        } catch (NumberFormatException _) {
            return Optional.empty();
        }
    }
}
