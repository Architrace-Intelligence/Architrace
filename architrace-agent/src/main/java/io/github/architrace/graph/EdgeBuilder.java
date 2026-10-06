/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import io.github.architrace.graph.GraphNode.DatabaseNode;
import io.github.architrace.graph.GraphNode.ExternalNode;
import io.github.architrace.graph.GraphNode.ServiceNode;
import io.github.architrace.graph.GraphNode.TopicNode;
import io.github.architrace.span.Peer;
import io.github.architrace.span.SpanKind;
import io.github.architrace.span.SpanRecord;
import java.time.Instant;
import java.time.InstantSource;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class EdgeBuilder {

    private final PendingSpanIndex pending;
    private final InstantSource clock;
    private final TopicFilter topics;

    public EdgeBuilder(PendingSpanIndex pending, InstantSource clock) {
        this(pending, clock, TopicFilter.none());
    }

    public EdgeBuilder(PendingSpanIndex pending, InstantSource clock, TopicFilter topics) {
        this.pending = Objects.requireNonNull(pending, "pending");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.topics = Objects.requireNonNull(topics, "topics");
    }

    public List<EdgeObservation> onSpan(SpanRecord span) {
        return switch (span.kind()) {
            case CLIENT -> onClient(span);
            case SERVER -> onServer(span);
            case PRODUCER ->
                topic(span)
                        .map(topic -> List.of(EdgeObservation.of(span, service(span), topic, EdgeKind.PUBLISH)))
                        .orElse(List.of());
            case CONSUMER ->
                topic(span)
                        .map(topic -> List.of(EdgeObservation.of(span, topic, service(span), EdgeKind.CONSUME)))
                        .orElse(List.of());
            case INTERNAL -> List.of();
        };
    }

    public Expiry expire(Instant now) {
        PendingSpanIndex.Expired expired = pending.expire(now);
        List<EdgeObservation> external = expired.clients().stream()
                .flatMap(client -> externalEdge(client).stream())
                .toList();
        int dropped = expired.clients().size() - external.size() + expired.servers();
        return new Expiry(external, dropped);
    }

    public int pendingSpans() {
        return pending.size();
    }

    private List<EdgeObservation> onClient(SpanRecord client) {
        return switch (client.peer()) {
            case Peer.Database database ->
                List.of(EdgeObservation.of(
                        client,
                        service(client),
                        new DatabaseNode(database.system(), database.namespace()),
                        EdgeKind.SYNC));
            case Peer.Messaging _ -> List.of();
            case Peer.Http _, Peer.None _ ->
                pending.takeServer(client.traceId(), client.spanId())
                        .map(server -> pair(client, server))
                        .orElseGet(() -> hold(client));
        };
    }

    private List<EdgeObservation> onServer(SpanRecord server) {
        return server.parentSpanId()
                .map(parent -> pending.takeClient(server.traceId(), parent)
                        .map(client -> pair(client, server))
                        .orElseGet(() -> hold(server)))
                .orElse(List.of());
    }

    private List<EdgeObservation> hold(SpanRecord span) {
        Instant now = clock.instant();
        if (span.kind() == SpanKind.CLIENT) {
            pending.holdClient(span, now);
        } else {
            pending.holdServer(span, now);
        }
        return List.of();
    }

    private static List<EdgeObservation> pair(SpanRecord client, SpanRecord server) {
        ServiceNode source = service(client);
        ServiceNode target = service(server);
        return source.equals(target) ? List.of() : List.of(EdgeObservation.of(client, source, target, EdgeKind.SYNC));
    }

    private static Optional<EdgeObservation> externalEdge(SpanRecord client) {
        return client.peer() instanceof Peer.Http(String address, Optional<Integer> _)
                ? Optional.of(EdgeObservation.of(client, service(client), new ExternalNode(address), EdgeKind.SYNC))
                : Optional.empty();
    }

    private Optional<TopicNode> topic(SpanRecord span) {
        return span.peer() instanceof Peer.Messaging(String system, Optional<String> destination, Optional<String> _)
                ? destination.filter(name -> !topics.ignores(name)).map(name -> new TopicNode(system, name))
                : Optional.empty();
    }

    private static ServiceNode service(SpanRecord span) {
        return new ServiceNode(span.service().domain(), span.service().name());
    }

    public record Expiry(List<EdgeObservation> externalEdges, int droppedSpans) {
        public Expiry {
            externalEdges = List.copyOf(externalEdges);
        }
    }
}
