/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.ingestion.grpc;

import io.github.architrace.control.plane.ingestion.IngestionMetrics;
import io.github.architrace.control.plane.ingestion.IngestionProperties;
import io.github.architrace.control.plane.ingestion.IngestionService;
import io.github.architrace.control.plane.topology.Agent;
import io.github.architrace.control.plane.topology.AgentLiveness;
import io.github.architrace.control.plane.topology.AgentStore;
import io.github.architrace.grpc.proto.AgentHealthRequest;
import io.github.architrace.grpc.proto.AgentHealthResponse;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.grpc.proto.ControlPlaneServiceGrpc;
import io.grpc.stub.StreamObserver;
import java.time.Clock;
import java.util.Comparator;
import java.util.Optional;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class AgentStreamService extends ControlPlaneServiceGrpc.ControlPlaneServiceImplBase {

    private final IngestionService ingestion;
    private final IngestionProperties properties;
    private final IngestionMetrics metrics;
    private final AgentStore agents;
    private final AgentLiveness liveness;
    private final Clock clock;

    public AgentStreamService(
            IngestionService ingestion,
            IngestionProperties properties,
            IngestionMetrics metrics,
            AgentStore agents,
            AgentLiveness liveness,
            Clock clock) {
        this.ingestion = ingestion;
        this.properties = properties;
        this.metrics = metrics;
        this.agents = agents;
        this.liveness = liveness;
        this.clock = clock;
    }

    @Override
    public StreamObserver<AgentRegisterRequestedEvent> connect(StreamObserver<ControlPlaneCommand> responseObserver) {
        return new AgentConnection(ingestion, properties, metrics, responseObserver);
    }

    @Override
    public void getAgentHealth(AgentHealthRequest request, StreamObserver<AgentHealthResponse> responseObserver) {
        Optional<Agent> latest = agents.all().stream()
                .filter(agent -> agent.name().equals(request.getAgentName()))
                .max(Comparator.comparing(Agent::lastSeenAt));
        AgentHealthResponse.Builder response = AgentHealthResponse.newBuilder();
        latest.ifPresent(agent -> response.setLive(liveness.isLive(agent, clock.instant()))
                .setLastSeenEpochMs(agent.lastSeenAt().toEpochMilli()));
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }
}
