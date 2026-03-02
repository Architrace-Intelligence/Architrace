/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.service.grpc;

import io.github.architrace.grpc.proto.AgentHealthRequest;
import io.github.architrace.grpc.proto.AgentHealthResponse;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.ConfigUpdate;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.grpc.proto.ControlPlaneServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
public class AgentService extends ControlPlaneServiceGrpc.ControlPlaneServiceImplBase {

  private static final Logger log = LoggerFactory.getLogger(AgentService.class);

  @Override
  public StreamObserver<AgentRegisterRequestedEvent> connect(
      StreamObserver<ControlPlaneCommand> responseObserver) {
    return new StreamObserver<>() {
      @Override
      public void onNext(AgentRegisterRequestedEvent agentEvent) {
        if (agentEvent.hasRegister()) {
          responseObserver.onNext(
              ControlPlaneCommand.newBuilder()
                  .setConfigUpdate(
                      ConfigUpdate.newBuilder()
                          .setVersion("1")
                          .putConfig("agent.mode", "managed")
                          .build())
                  .build());
        }
      }

      @Override
      public void onError(Throwable throwable) {
        log.warn("Agent stream failed: {}", throwable.getMessage(), throwable);
      }

      @Override
      public void onCompleted() {
        responseObserver.onCompleted();
      }
    };
  }

  @Override
  public void getAgentHealth(
      AgentHealthRequest request, StreamObserver<AgentHealthResponse> responseObserver) {
    responseObserver.onNext(
        AgentHealthResponse.newBuilder()
            .setLive(true)
            .setLastSeenEpochMs(System.currentTimeMillis())
            .build());
    responseObserver.onCompleted();
  }
}

