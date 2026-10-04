/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.service.graph;

import io.github.architrace.model.AsyncKey;
import io.github.architrace.span.Peer;
import io.github.architrace.span.SpanKind;
import io.github.architrace.span.SpanRecord;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AsyncDependencyResolver extends AbstractDependencyResolver {

    private final ConcurrentMap<AsyncKey, SpanRecord> waitingProducers = new ConcurrentHashMap<>();

    public void onSpan(SpanRecord span) {
        destination(span).ifPresent(destination -> route(span, new AsyncKey(span.traceId(), destination)));
    }

    private void route(SpanRecord span, AsyncKey key) {
        if (span.kind() == SpanKind.PRODUCER) {
            waitingProducers.put(key, span);
        } else if (span.kind() == SpanKind.CONSUMER) {
            Optional.ofNullable(waitingProducers.remove(key)).ifPresent(producer -> buildDependency(producer, span));
        }
    }

    private static Optional<String> destination(SpanRecord span) {
        return span.peer() instanceof Peer.Messaging messaging ? messaging.destination() : Optional.empty();
    }
}
