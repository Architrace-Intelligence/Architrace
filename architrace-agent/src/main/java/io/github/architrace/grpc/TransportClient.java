/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.grpc;

import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.grpc.stub.StreamObserver;

public interface TransportClient extends AutoCloseable {

  StreamObserver<AgentRegisterRequestedEvent> open(
      StreamObserver<ControlPlaneCommand> inboundObserver);

  @Override
  void close();
}

