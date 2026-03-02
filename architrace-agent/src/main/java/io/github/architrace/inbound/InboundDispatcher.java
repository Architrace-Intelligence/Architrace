/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.inbound;

import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.session.ControlPlaneSession;
import java.util.List;
import java.util.Objects;

public class InboundDispatcher {
  private final List<ControlMessageHandler> handlers;

  public InboundDispatcher(List<ControlMessageHandler> handlers) {
    this.handlers = List.copyOf(Objects.requireNonNull(handlers, "handlers"));
  }

  public void dispatch(ControlPlaneCommand msg, ControlPlaneSession session) {
    handlers.stream()
        .filter(h -> h.supports(msg))
        .findFirst()
        .ifPresent(h -> h.handle(msg, session));
  }
}
