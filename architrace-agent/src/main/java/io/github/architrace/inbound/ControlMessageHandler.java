/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.inbound;

import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.session.ControlPlaneSession;

public interface ControlMessageHandler {

  boolean supports(ControlPlaneCommand command);

  void handle(ControlPlaneCommand command, ControlPlaneSession session);
}
