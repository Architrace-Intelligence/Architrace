/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.grpc;

import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.session.ControlPlaneSession;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;

public class ControlPlaneStreamObserver implements StreamObserver<ControlPlaneCommand> {

  private final ControlPlaneSession session;
  private final CompletableFuture<Void> streamClosed;

  public ControlPlaneStreamObserver(ControlPlaneSession session, CompletableFuture<Void> streamClosed) {
    this.session = session;
    this.streamClosed = streamClosed;
  }

  @Override
  public void onNext(ControlPlaneCommand value) {
    session.handleInbound(value);
  }

  @Override
  public void onError(Throwable t) {
    session.handleError(t);
    streamClosed.completeExceptionally(t);
  }

  @Override
  public void onCompleted() {
    session.handleComplete();
    streamClosed.complete(null);
  }
}
