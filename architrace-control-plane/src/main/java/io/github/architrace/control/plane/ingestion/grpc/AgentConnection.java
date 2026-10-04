/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.ingestion.grpc;

import io.github.architrace.control.plane.ingestion.IngestionMetrics;
import io.github.architrace.control.plane.ingestion.IngestionProperties;
import io.github.architrace.control.plane.ingestion.IngestionService;
import io.github.architrace.control.plane.ingestion.InvalidSnapshotException;
import io.github.architrace.control.plane.topology.Agent;
import io.github.architrace.control.plane.topology.AgentRegistration;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.SnapshotId;
import io.github.architrace.grpc.proto.AgentRegister;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.ConfigUpdate;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.grpc.proto.GraphSnapshot;
import io.github.architrace.grpc.proto.SnapshotAck;
import io.github.architrace.grpc.proto.SnapshotRejected;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AgentConnection implements StreamObserver<AgentRegisterRequestedEvent> {

    private static final Logger log = LoggerFactory.getLogger(AgentConnection.class);
    private static final String CONFIG_VERSION = "1";

    private final IngestionService ingestion;
    private final IngestionProperties properties;
    private final IngestionMetrics metrics;
    private final StreamObserver<ControlPlaneCommand> responses;
    private Agent agent;
    private boolean closed;

    AgentConnection(
            IngestionService ingestion,
            IngestionProperties properties,
            IngestionMetrics metrics,
            StreamObserver<ControlPlaneCommand> responses) {
        this.ingestion = ingestion;
        this.properties = properties;
        this.metrics = metrics;
        this.responses = responses;
    }

    @Override
    public void onNext(AgentRegisterRequestedEvent event) {
        if (closed) {
            return;
        }
        switch (event.getPayloadCase()) {
            case REGISTER -> register(event.getRegister());
            case SNAPSHOT -> snapshot(event.getSnapshot());
            case HEARTBEAT -> heartbeat();
            case PAYLOAD_NOT_SET -> reject(0, "empty message");
        }
    }

    @Override
    public void onError(Throwable throwable) {
        log.atWarn()
                .setMessage("Agent stream of {} failed: {}")
                .addArgument(this::describe)
                .addArgument(throwable::getMessage)
                .log();
        disconnect();
    }

    @Override
    public void onCompleted() {
        disconnect();
        responses.onCompleted();
    }

    private void register(AgentRegister register) {
        if (agent != null) {
            fail(Status.FAILED_PRECONDITION.withDescription("agent is already registered"));
            return;
        }
        try {
            AgentRegistration registration = new AgentRegistration(
                    register.getAgentName(),
                    register.getAgentVersion(),
                    new Scope(register.getProject(), register.getEnvironment(), register.getClusterId()));
            agent = ingestion.register(registration);
        } catch (IllegalArgumentException e) {
            fail(Status.INVALID_ARGUMENT.withDescription("invalid registration: " + e.getMessage()));
            return;
        }
        metrics.agentConnected();
        log.info("Agent {} registered for scope {}", agent.name(), agent.scope());
        responses.onNext(ControlPlaneCommand.newBuilder()
                .setConfigUpdate(ConfigUpdate.newBuilder()
                        .setVersion(CONFIG_VERSION)
                        .putConfig(
                                "snapshot.interval-seconds",
                                Long.toString(properties.snapshotInterval().toSeconds()))
                        .putConfig(
                                "heartbeat.interval-seconds",
                                Long.toString(properties.heartbeatInterval().toSeconds())))
                .build());
    }

    private void snapshot(GraphSnapshot snapshot) {
        if (!requireRegistered()) {
            return;
        }
        try {
            SnapshotId id = ingestion.ingest(agent, snapshot);
            responses.onNext(ControlPlaneCommand.newBuilder()
                    .setSnapshotAck(SnapshotAck.newBuilder()
                            .setWindowEndEpochMs(snapshot.getWindowEndEpochMs())
                            .setSnapshotId(id.value()))
                    .build());
        } catch (InvalidSnapshotException e) {
            log.atWarn()
                    .setMessage("Rejected snapshot from {}: {}")
                    .addArgument(this::describe)
                    .addArgument(e::getMessage)
                    .log();
            reject(snapshot.getWindowEndEpochMs(), e.getMessage());
        }
    }

    private void heartbeat() {
        if (requireRegistered()) {
            ingestion.heartbeat(agent.id());
        }
    }

    private boolean requireRegistered() {
        if (agent == null) {
            fail(Status.FAILED_PRECONDITION.withDescription("register before sending snapshots"));
            return false;
        }
        return true;
    }

    private void reject(long windowEndEpochMs, String reason) {
        responses.onNext(ControlPlaneCommand.newBuilder()
                .setSnapshotRejected(SnapshotRejected.newBuilder()
                        .setWindowEndEpochMs(windowEndEpochMs)
                        .setReason(reason == null ? "invalid snapshot" : reason))
                .build());
    }

    private void fail(Status status) {
        disconnect();
        responses.onError(status.asRuntimeException());
    }

    private void disconnect() {
        if (closed) {
            return;
        }
        closed = true;
        if (agent != null) {
            metrics.agentDisconnected();
        }
    }

    private String describe() {
        return agent == null ? "an unregistered agent" : agent.name() + " " + agent.scope();
    }
}
