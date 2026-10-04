/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import io.github.architrace.span.SpanRecord;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class PendingSpanIndex {

    private final Duration ttl;
    private final Map<TraceSpanKey, Entry> clientsBySpan = new HashMap<>();
    private final Map<TraceSpanKey, Entry> serversByParent = new HashMap<>();
    private volatile int held;

    public PendingSpanIndex(Duration ttl) {
        this.ttl = Objects.requireNonNull(ttl, "ttl");
    }

    public void holdClient(SpanRecord client, Instant now) {
        clientsBySpan.put(new TraceSpanKey(client.traceId(), client.spanId()), new Entry(client, now.plus(ttl)));
        held = clientsBySpan.size() + serversByParent.size();
    }

    public void holdServer(SpanRecord server, Instant now) {
        server.parentSpanId()
                .ifPresent(parent -> serversByParent.put(
                        new TraceSpanKey(server.traceId(), parent), new Entry(server, now.plus(ttl))));
        held = clientsBySpan.size() + serversByParent.size();
    }

    public Optional<SpanRecord> takeClient(String traceId, String spanId) {
        Optional<SpanRecord> client = Optional.ofNullable(clientsBySpan.remove(new TraceSpanKey(traceId, spanId)))
                .map(Entry::span);
        held = clientsBySpan.size() + serversByParent.size();
        return client;
    }

    public Optional<SpanRecord> takeServer(String traceId, String parentSpanId) {
        Optional<SpanRecord> server = Optional.ofNullable(
                        serversByParent.remove(new TraceSpanKey(traceId, parentSpanId)))
                .map(Entry::span);
        held = clientsBySpan.size() + serversByParent.size();
        return server;
    }

    public Expired expire(Instant now) {
        Expired expired = new Expired(
                evict(clientsBySpan, now), evict(serversByParent, now).size());
        held = clientsBySpan.size() + serversByParent.size();
        return expired;
    }

    public int size() {
        return held;
    }

    private static List<SpanRecord> evict(Map<TraceSpanKey, Entry> entries, Instant now) {
        List<SpanRecord> expired = entries.values().stream()
                .filter(entry -> !entry.deadline().isAfter(now))
                .map(Entry::span)
                .toList();
        entries.values().removeIf(entry -> !entry.deadline().isAfter(now));
        return expired;
    }

    public record Expired(List<SpanRecord> clients, int servers) {
        public Expired {
            clients = List.copyOf(clients);
        }
    }

    private record Entry(SpanRecord span, Instant deadline) {}
}
