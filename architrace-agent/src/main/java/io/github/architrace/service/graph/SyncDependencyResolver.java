/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.service.graph;

import io.github.architrace.model.TraceParentKey;
import io.github.architrace.span.SpanKind;
import io.github.architrace.span.SpanRecord;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SyncDependencyResolver extends AbstractDependencyResolver {

    private final GlobalSpanRegistry registry;
    private final ConcurrentMap<TraceParentKey, SpanRecord> waitingServers = new ConcurrentHashMap<>();

    public SyncDependencyResolver(GlobalSpanRegistry registry) {
        this.registry = registry;
    }

    public void onSpan(SpanRecord span) {
        if (!registry.registerIfAbsent(span)) {
            return;
        }
        switch (span.kind()) {
            case SERVER -> handleServer(span);
            case CLIENT -> handleClient(span);
            case PRODUCER, CONSUMER, INTERNAL -> {}
        }
    }

    private void handleServer(SpanRecord server) {
        Optional<SpanRecord> client = registry.findParent(server).filter(parent -> parent.kind() == SpanKind.CLIENT);
        client.ifPresentOrElse(
                parent -> buildDependency(parent, server),
                () -> server.parentSpanId()
                        .ifPresent(parentId ->
                                waitingServers.put(new TraceParentKey(server.traceId(), parentId), server)));
    }

    private void handleClient(SpanRecord client) {
        Optional.ofNullable(waitingServers.remove(new TraceParentKey(client.traceId(), client.spanId())))
                .ifPresent(server -> buildDependency(client, server));
    }
}
