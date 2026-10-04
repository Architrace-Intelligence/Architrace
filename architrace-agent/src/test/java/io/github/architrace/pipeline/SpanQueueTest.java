/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.span.SpanKind;
import io.github.architrace.span.SpanRecord;
import io.github.architrace.testsupport.TestSpans;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpanQueueTest {

    private final SpanRecord span = TestSpans.span(SpanKind.CLIENT, "t", "c1", "checkout");

    @Test
    void rejectsAndCountsSpansBeyondTheCapacity() throws InterruptedException {
        SpanQueue sut = new SpanQueue(2);

        sut.offerAll(List.of(span, span, span));

        assertThat(sut.size()).isEqualTo(2);
        assertThat(sut.rejected()).isEqualTo(1);
        assertThat(sut.poll(Duration.ofMillis(10))).isSameAs(span);
        assertThat(sut.offer(span)).isTrue();
        List<SpanRecord> drained = new ArrayList<>();
        assertThat(sut.drainTo(drained, 10)).isEqualTo(2);
        assertThat(sut.poll(Duration.ofMillis(10))).isNull();
    }
}
