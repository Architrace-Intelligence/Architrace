/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.span.SpanKind;
import io.github.architrace.span.SpanRecord;
import io.github.architrace.testsupport.TestSpans;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PendingSpanIndexTest {

    private static final Instant T0 = Instant.parse("2026-10-04T10:00:00Z");

    private final PendingSpanIndex sut = new PendingSpanIndex(Duration.ofSeconds(120));

    @Test
    void heldSpansAreTakenOnceByTheirPairingKey() {
        SpanRecord client = TestSpans.span(SpanKind.CLIENT, "t", "c1", "checkout");
        SpanRecord server = TestSpans.child(SpanKind.SERVER, "t", "s1", "c1", "orders");
        sut.holdClient(client, T0);
        sut.holdServer(server, T0);

        assertThat(sut.size()).isEqualTo(2);
        assertThat(sut.takeClient("t", "c1")).contains(client);
        assertThat(sut.takeClient("t", "c1")).isEmpty();
        assertThat(sut.takeServer("t", "c1")).contains(server);
        assertThat(sut.takeServer("t", "other")).isEmpty();
        assertThat(sut.size()).isZero();
    }

    @Test
    void rootServersAreNeverHeld() {
        sut.holdServer(TestSpans.span(SpanKind.SERVER, "t", "s1", "orders"), T0);

        assertThat(sut.size()).isZero();
    }

    @Test
    void expiryReturnsClientsPastTheirDeadlineAndCountsServers() {
        SpanRecord oldClient = TestSpans.span(SpanKind.CLIENT, "t", "c1", "checkout");
        SpanRecord freshClient = TestSpans.span(SpanKind.CLIENT, "t", "c2", "checkout");
        SpanRecord oldServer = TestSpans.child(SpanKind.SERVER, "t", "s1", "x1", "orders");
        sut.holdClient(oldClient, T0);
        sut.holdServer(oldServer, T0);
        sut.holdClient(freshClient, T0.plusSeconds(60));

        PendingSpanIndex.Expired expired = sut.expire(T0.plusSeconds(120));

        assertThat(expired.clients()).containsExactly(oldClient);
        assertThat(expired.servers()).isEqualTo(1);
        assertThat(sut.size()).isEqualTo(1);
        assertThat(sut.takeClient("t", "c2")).contains(freshClient);
    }
}
