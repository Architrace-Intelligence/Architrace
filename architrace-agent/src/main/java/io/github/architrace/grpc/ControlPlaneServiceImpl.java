/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.grpc;

import io.github.architrace.controlplane.ControlPlaneRegistry;
import io.github.architrace.grpc.proto.AgentHealthRequest;
import io.github.architrace.grpc.proto.AgentHealthResponse;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.grpc.proto.ControlPlaneServiceGrpc;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlPlaneServiceImpl extends ControlPlaneServiceGrpc.ControlPlaneServiceImplBase
    implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(ControlPlaneServiceImpl.class);
  private static final long LIVE_THRESHOLD_MS = 15_000L;

  private final ControlPlaneRegistry registry;
  private final ScheduledExecutorService tickExecutor;

  public ControlPlaneServiceImpl(ControlPlaneRegistry registry) {
    this.registry = registry;
    this.tickExecutor = Executors.newSingleThreadScheduledExecutor();
    this.tickExecutor.scheduleAtFixedRate(registry::tick, 2, 5, TimeUnit.SECONDS);
  }

  @Override
  public StreamObserver<AgentRegisterRequestedEvent> connect(
      StreamObserver<ControlPlaneCommand> responseObserver) {
    return new StreamObserver<>() {
      private String agentName;

      @Override
      public void onNext(AgentRegisterRequestedEvent agentEvent) {
        if (agentEvent.hasRegister()) {
          agentName = agentEvent.getRegister().getAgentName();
          registry.register(agentName, responseObserver);
          log.info("Agent '{}' connected to control plane.", agentName);
          return;
        }

        if (agentEvent.hasGraphBatch()) {
          log.info(
              "Received graph batch from agent='{}' nodes={} edges={}.",
              agentEvent.getGraphBatch().getAgentName(),
              agentEvent.getGraphBatch().getNodesCount(),
              agentEvent.getGraphBatch().getEdgesCount());
        }
      }

      @Override
      public void onError(Throwable throwable) {
        registry.unregister(agentName);
        log.warn("Agent stream failed for '{}'", agentName, throwable);
      }

      @Override
      public void onCompleted() {
        registry.touch(agentName);
        registry.unregister(agentName);
        responseObserver.onCompleted();
        log.info("Agent '{}' disconnected.", agentName);
      }
    };
  }

  @Override
  public void getAgentHealth(
      AgentHealthRequest request, StreamObserver<AgentHealthResponse> responseObserver) {
    var health = registry.health(request.getAgentName(), LIVE_THRESHOLD_MS);
    responseObserver.onNext(
        AgentHealthResponse.newBuilder()
            .setLive(health.live())
            .setLastSeenEpochMs(health.lastSeenEpochMs())
            .build());
    responseObserver.onCompleted();
  }

  @Override
  public void close() {
    tickExecutor.shutdownNow();
  }
}
