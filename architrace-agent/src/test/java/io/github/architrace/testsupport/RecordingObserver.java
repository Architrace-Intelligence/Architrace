/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.testsupport;

import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RecordingObserver<T> implements StreamObserver<T> {

    private final List<T> values = new CopyOnWriteArrayList<>();
    private final List<Throwable> errors = new CopyOnWriteArrayList<>();
    private volatile boolean completed;

    @Override
    public void onNext(T value) {
        values.add(value);
    }

    @Override
    public void onError(Throwable throwable) {
        errors.add(throwable);
    }

    @Override
    public void onCompleted() {
        completed = true;
    }

    public List<T> values() {
        return List.copyOf(values);
    }

    public List<Throwable> errors() {
        return List.copyOf(errors);
    }

    public boolean isCompleted() {
        return completed;
    }
}
