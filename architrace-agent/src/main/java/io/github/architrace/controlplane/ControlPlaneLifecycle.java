/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.controlplane;

import io.github.architrace.config.ControlPlaneClientWiringConfig;
import io.github.architrace.grpc.ControlPlaneStreamObserver;
import io.github.architrace.grpc.TransportClient;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.GraphBatch;
import io.github.architrace.inbound.ControlMessageHandler;
import io.github.architrace.session.ControlPlaneSession;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.StructuredTaskScope;

public final class ControlPlaneLifecycle implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(ControlPlaneLifecycle.class);

  private final String agentName;
  private final TransportClient transportClient;
  private final RegistrationService registrationService;
  private final ControlPlaneSession session;
  private final CompletableFuture<Void> termination = new CompletableFuture<>();
  private final CompletableFuture<Void> streamClosed = new CompletableFuture<>();

  public ControlPlaneLifecycle(
      String agentName,
      TransportClient transportClient,
      RegistrationService registrationService,
      List<ControlMessageHandler> handlers) {

    this.agentName = agentName;
    this.transportClient = transportClient;
    this.registrationService = registrationService;

    var wiring = ControlPlaneClientWiringConfig.create(
        handlers,
        () -> termination.complete(null),
        termination::completeExceptionally
    );

    this.session = wiring.session();
  }

  public void await() {
    termination.join();
  }

  public void publishGraphBatch(GraphBatch graphBatch) {
    session.send(
        AgentRegisterRequestedEvent.newBuilder()
            .setGraphBatch(graphBatch)
            .build()
    );
  }

  @Override
  public void close() {
    try {
      transportClient.close();
    } finally {
      termination.complete(null);
    }
  }

  public void run() {
    try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {
      var observer = transportClient.open(new ControlPlaneStreamObserver(session, streamClosed));
      registrationService.sendRegister(agentName, observer);
      log.info("Control-plane session started for agent='{}'.", agentName);

      scope.fork(() -> outboundWriterTask(observer));
      scope.fork(this::heartbeatTask);
      scope.fork(this::grpcBlockingTask);
      scope.join();

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      transportClient.close();
    }
  }

  /**
   * Waits until stream ends.
   */
  private Void grpcBlockingTask() throws Exception {
    streamClosed.get();

    return null;
  }

  /**
   * Sends outbound messages from queue to gRPC stream.
   */
  private Void outboundWriterTask(StreamObserver<AgentRegisterRequestedEvent> observer) throws InterruptedException {
    while (!termination.isDone()) {
      var event = session.takeOutbound(); // blocking (virtual thread)
      observer.onNext(event);
    }

    return null;
  }

  private Void heartbeatTask() throws InterruptedException {
    while (!termination.isDone()) {
      Thread.sleep(15_000);
    }

    return null;
  }
}