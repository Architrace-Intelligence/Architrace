/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.otlp;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.model.NodeType;
import io.github.architrace.span.Peer;
import io.github.architrace.span.SpanKind;
import io.github.architrace.testsupport.TestSpans;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NodeExtractorTest {

    private final NodeExtractor sut = new NodeExtractor();

    @Test
    void databasePeerBecomesADatabaseNode() {
        NodeDescriptor node = sut.extract(TestSpans.withPeer(
                SpanKind.CLIENT, "t", "s", "orders", new Peer.Database("postgresql", Optional.of("orders"))));

        assertThat(node).isEqualTo(new NodeDescriptor("db:postgresql", NodeType.DATABASE, "postgresql"));
    }

    @Test
    void messagingPeerWithADestinationBecomesATopicNode() {
        NodeDescriptor node = sut.extract(TestSpans.withPeer(
                SpanKind.PRODUCER,
                "t",
                "s",
                "orders",
                new Peer.Messaging("kafka", Optional.of("orders"), Optional.empty())));

        assertThat(node).isEqualTo(new NodeDescriptor("topic:orders", NodeType.TOPIC, "orders"));
    }

    @Test
    void messagingPeerWithoutADestinationFallsBackToTheService() {
        NodeDescriptor node = sut.extract(TestSpans.withPeer(
                SpanKind.PRODUCER,
                "t",
                "s",
                "orders",
                new Peer.Messaging("kafka", Optional.empty(), Optional.empty())));

        assertThat(node).isEqualTo(new NodeDescriptor("DEV:default:orders", NodeType.SERVICE, "orders"));
    }

    @Test
    void httpPeerOfAClientSpanBecomesAnExternalNode() {
        NodeDescriptor node = sut.extract(TestSpans.withPeer(
                SpanKind.CLIENT, "t", "s", "orders", new Peer.Http("api.example.com", Optional.of(443))));

        assertThat(node)
                .isEqualTo(new NodeDescriptor("ext:api.example.com", NodeType.EXTERNAL_SERVICE, "api.example.com"));
    }

    @Test
    void httpPeerOfAServerSpanStaysAServiceNode() {
        NodeDescriptor node = sut.extract(TestSpans.withPeer(
                SpanKind.SERVER, "t", "s", "orders", new Peer.Http("orders.internal", Optional.empty())));

        assertThat(node).isEqualTo(new NodeDescriptor("DEV:default:orders", NodeType.SERVICE, "orders"));
    }

    @Test
    void spanWithoutAPeerIsItsOwnServiceNode() {
        NodeDescriptor node = sut.extract(TestSpans.span(SpanKind.INTERNAL, "t", "s", "orders"));

        assertThat(node).isEqualTo(new NodeDescriptor("DEV:default:orders", NodeType.SERVICE, "orders"));
    }
}
