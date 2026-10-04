/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.graph.GraphNode.DatabaseNode;
import io.github.architrace.graph.GraphNode.ExternalNode;
import io.github.architrace.graph.GraphNode.ServiceNode;
import io.github.architrace.graph.GraphNode.TopicNode;
import io.github.architrace.span.Peer;
import io.github.architrace.span.SpanKind;
import io.github.architrace.span.SpanRecord;
import io.github.architrace.testsupport.MutableClock;
import io.github.architrace.testsupport.TestSpans;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EdgeBuilderTest {

    private static final ServiceNode CHECKOUT = new ServiceNode("default", "checkout");
    private static final ServiceNode ORDERS = new ServiceNode("default", "orders");
    private static final Peer.Http ORDERS_HTTP = new Peer.Http("orders.svc", Optional.of(8080));
    private static final Peer.Messaging ORDERS_TOPIC =
            new Peer.Messaging("kafka", Optional.of("orders"), Optional.of("publish"));

    private final MutableClock clock = MutableClock.startingAt("2026-10-04T10:00:00Z");
    private final EdgeBuilder sut = new EdgeBuilder(new PendingSpanIndex(Duration.ofSeconds(120)), clock);

    @Test
    void clientThenServerPairsIntoASyncEdgeWithTheClientMetrics() {
        SpanRecord client = TestSpans.withPeer(SpanKind.CLIENT, "t", "c1", "checkout", ORDERS_HTTP);
        SpanRecord server = TestSpans.child(SpanKind.SERVER, "t", "s1", "c1", "orders");

        assertThat(sut.onSpan(client)).isEmpty();
        assertThat(sut.pendingSpans()).isEqualTo(1);
        assertThat(sut.onSpan(server))
                .containsExactly(new EdgeObservation(new EdgeKey(CHECKOUT, ORDERS, EdgeKind.SYNC), 250, false));
        assertThat(sut.pendingSpans()).isZero();
    }

    @Test
    void serverThenClientPairsTheSameWay() {
        SpanRecord server = TestSpans.child(SpanKind.SERVER, "t", "s1", "c1", "orders");
        SpanRecord client = TestSpans.withPeer(SpanKind.CLIENT, "t", "c1", "checkout", new Peer.None());

        assertThat(sut.onSpan(server)).isEmpty();
        assertThat(sut.onSpan(client))
                .containsExactly(new EdgeObservation(new EdgeKey(CHECKOUT, ORDERS, EdgeKind.SYNC), 250, false));
    }

    @Test
    void callsWithinOneServiceAndRootServersProduceNothing() {
        SpanRecord client = TestSpans.span(SpanKind.CLIENT, "t", "c1", "checkout");
        SpanRecord server = TestSpans.child(SpanKind.SERVER, "t", "s1", "c1", "checkout");
        SpanRecord root = TestSpans.span(SpanKind.SERVER, "t", "s0", "checkout");
        SpanRecord internal = TestSpans.child(SpanKind.INTERNAL, "t", "i1", "s0", "checkout");

        assertThat(sut.onSpan(client)).isEmpty();
        assertThat(sut.onSpan(server)).isEmpty();
        assertThat(sut.onSpan(root)).isEmpty();
        assertThat(sut.onSpan(internal)).isEmpty();
        assertThat(sut.pendingSpans()).isZero();
    }

    @Test
    void databaseClientsBuildAnImmediateEdge() {
        SpanRecord client = TestSpans.withPeer(
                SpanKind.CLIENT, "t", "c1", "orders", new Peer.Database("postgresql", Optional.of("orders")));

        assertThat(sut.onSpan(client))
                .containsExactly(new EdgeObservation(
                        new EdgeKey(ORDERS, new DatabaseNode("postgresql", Optional.of("orders")), EdgeKind.SYNC),
                        250,
                        false));
        assertThat(sut.pendingSpans()).isZero();
    }

    @Test
    void producersAndConsumersBuildTopicEdges() {
        SpanRecord producer = TestSpans.withPeer(SpanKind.PRODUCER, "t", "p1", "checkout", ORDERS_TOPIC);
        SpanRecord consumer = TestSpans.withPeer(SpanKind.CONSUMER, "t", "k1", "orders", ORDERS_TOPIC);
        TopicNode topic = new TopicNode("kafka", "orders");

        assertThat(sut.onSpan(producer))
                .containsExactly(new EdgeObservation(new EdgeKey(CHECKOUT, topic, EdgeKind.PUBLISH), 250, false));
        assertThat(sut.onSpan(consumer))
                .containsExactly(new EdgeObservation(new EdgeKey(topic, ORDERS, EdgeKind.CONSUME), 250, false));
    }

    @Test
    void messagingSpansWithoutADestinationAndMessagingClientsAreIgnored() {
        Peer.Messaging noDestination = new Peer.Messaging("kafka", Optional.empty(), Optional.empty());

        assertThat(sut.onSpan(TestSpans.withPeer(SpanKind.PRODUCER, "t", "p1", "checkout", noDestination)))
                .isEmpty();
        assertThat(sut.onSpan(TestSpans.withPeer(SpanKind.CLIENT, "t", "c1", "checkout", ORDERS_TOPIC)))
                .isEmpty();
        assertThat(sut.pendingSpans()).isZero();
    }

    @Test
    void expiredHttpClientsBecomeExternalEdgesAndOtherExpiredSpansAreDropped() {
        SpanRecord http = TestSpans.withPeer(SpanKind.CLIENT, "t", "c1", "checkout", ORDERS_HTTP);
        SpanRecord plain = TestSpans.withPeer(SpanKind.CLIENT, "t", "c2", "checkout", new Peer.None());
        SpanRecord orphanServer = TestSpans.child(SpanKind.SERVER, "t", "s1", "x1", "orders");
        List.of(http, plain, orphanServer).forEach(sut::onSpan);

        assertThat(sut.expire(clock.instant().plusSeconds(119))).isEqualTo(new EdgeBuilder.Expiry(List.of(), 0));
        EdgeBuilder.Expiry expiry = sut.expire(clock.instant().plusSeconds(120));

        assertThat(expiry.externalEdges())
                .containsExactly(new EdgeObservation(
                        new EdgeKey(CHECKOUT, new ExternalNode("orders.svc"), EdgeKind.SYNC), 250, false));
        assertThat(expiry.droppedSpans()).isEqualTo(2);
        assertThat(sut.pendingSpans()).isZero();
    }

    @Test
    void errorsAndLatenciesComeFromTheObservedSpan() {
        SpanRecord failed = new SpanRecord(
                "t",
                "c1",
                Optional.empty(),
                SpanKind.CLIENT,
                1_000_000_000L,
                1_040_000_000L,
                true,
                TestSpans.span(SpanKind.CLIENT, "t", "c1", "orders").service(),
                TestSpans.span(SpanKind.CLIENT, "t", "c1", "orders").deployment(),
                new Peer.Database("redis", Optional.empty()));

        assertThat(sut.onSpan(failed)).singleElement().satisfies(observation -> {
            assertThat(observation.latencyMillis()).isEqualTo(40);
            assertThat(observation.error()).isTrue();
        });
    }
}
