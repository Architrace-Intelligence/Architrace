/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.config;

import io.github.architrace.inbound.ControlMessageHandler;
import io.github.architrace.inbound.InboundDispatcher;
import io.github.architrace.outbound.OutboundDispatcher;
import io.github.architrace.outbound.OverflowStrategy;
import io.github.architrace.session.ControlPlaneSession;
import java.util.List;
import java.util.function.Consumer;

public final class ControlPlaneClientWiringConfig {

  private static final int OUTBOUND_QUEUE_CAPACITY = 512;
  private static final OverflowStrategy OUTBOUND_OVERFLOW_STRATEGY = OverflowStrategy.DROP_OLDEST;

  private ControlPlaneClientWiringConfig() {
  }

  public static ClientWiring create(
      List<ControlMessageHandler> handlers,
      Runnable onCompleted,
      Consumer<Throwable> onError) {

    OutboundDispatcher outbound =
        new OutboundDispatcher(
            OUTBOUND_QUEUE_CAPACITY,
            OUTBOUND_OVERFLOW_STRATEGY);

    ControlPlaneSession session =
        new ControlPlaneSession(
            new InboundDispatcher(List.copyOf(handlers)),
            outbound,
            onCompleted,
            onError);

    return new ClientWiring(session, outbound);
  }

  public record ClientWiring(ControlPlaneSession session, OutboundDispatcher outbound) {
  }
}
