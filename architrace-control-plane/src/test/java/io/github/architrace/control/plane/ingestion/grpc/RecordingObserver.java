/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.ingestion.grpc;

import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;

final class RecordingObserver<T> implements StreamObserver<T> {

  final List<T> values = new ArrayList<>();
  Throwable error;
  boolean completed;

  @Override
  public void onNext(T value) {
    values.add(value);
  }

  @Override
  public void onError(Throwable throwable) {
    error = throwable;
  }

  @Override
  public void onCompleted() {
    completed = true;
  }
}
