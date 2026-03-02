/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.grpc;

import io.github.architrace.grpc.proto.AgentRegister;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.ConfigUpdate;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.inbound.ControlMessageHandler;
import io.github.architrace.inbound.InboundDispatcher;
import io.github.architrace.outbound.OutboundDispatcher;
import io.github.architrace.outbound.OverflowStrategy;
import io.github.architrace.session.ControlPlaneSession;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentControlPlaneClientTest {

  @Test
  void onNextShouldDispatchInboundAndProduceOutboundEvent() throws Exception {
    AtomicReference<ControlPlaneCommand> handled = new AtomicReference<>();
    ControlMessageHandler handler = new ControlMessageHandler() {
      @Override
      public boolean supports(ControlPlaneCommand command) {
        return command.hasConfigUpdate();
      }

      @Override
      public void handle(ControlPlaneCommand command, ControlPlaneSession session) {
        handled.set(command);
        session.send(
            AgentRegisterRequestedEvent.newBuilder()
                .setRegister(AgentRegister.newBuilder().setAgentName("agent-a").build())
                .build());
      }
    };

    ControlPlaneSession session =
        new ControlPlaneSession(
            new InboundDispatcher(List.of(handler)),
            new OutboundDispatcher(4, OverflowStrategy.BLOCK),
            () -> {
            },
            throwable -> {
            });
    CompletableFuture<Void> streamClosed = new CompletableFuture<>();
    ControlPlaneStreamObserver observer = new ControlPlaneStreamObserver(session, streamClosed);

    ControlPlaneCommand command =
        ControlPlaneCommand.newBuilder()
            .setConfigUpdate(
                ConfigUpdate.newBuilder()
                    .setVersion("42")
                    .putConfig("k", "v")
                    .build())
            .build();

    observer.onNext(command);

    assertThat(handled.get()).isEqualTo(command);
    AgentRegisterRequestedEvent outbound = session.takeOutbound();
    assertThat(outbound.hasRegister()).isTrue();
    assertThat(outbound.getRegister().getAgentName()).isEqualTo("agent-a");
    assertThat(streamClosed).isNotCompleted();
  }

  @Test
  void onCompletedShouldCompleteSessionAndStreamFuture() {
    AtomicBoolean completed = new AtomicBoolean(false);
    ControlPlaneSession session =
        new ControlPlaneSession(
            new InboundDispatcher(List.of()),
            new OutboundDispatcher(1, OverflowStrategy.BLOCK),
            () -> completed.set(true),
            throwable -> {
            });
    CompletableFuture<Void> streamClosed = new CompletableFuture<>();
    ControlPlaneStreamObserver observer = new ControlPlaneStreamObserver(session, streamClosed);

    observer.onCompleted();

    assertThat(completed.get()).isTrue();
    assertThat(streamClosed).isCompleted();
    assertThatCode(streamClosed::join).doesNotThrowAnyException();
  }

  @Test
  void onErrorShouldPropagateToSessionAndStreamFuture() {
    AtomicReference<Throwable> failure = new AtomicReference<>();
    ControlPlaneSession session =
        new ControlPlaneSession(
            new InboundDispatcher(List.of()),
            new OutboundDispatcher(1, OverflowStrategy.BLOCK),
            () -> {
            },
            failure::set);
    CompletableFuture<Void> streamClosed = new CompletableFuture<>();
    ControlPlaneStreamObserver observer = new ControlPlaneStreamObserver(session, streamClosed);
    RuntimeException boom = new RuntimeException("boom");

    observer.onError(boom);

    assertThat(failure.get()).isSameAs(boom);
    assertThat(streamClosed).isCompletedExceptionally();
    assertThatThrownBy(streamClosed::join)
        .hasCauseInstanceOf(RuntimeException.class)
        .hasMessageContaining("boom");
  }
}
