/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.publish;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.graph.GraphSnapshot;
import io.github.architrace.testsupport.TestSnapshots;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class SnapshotQueueTest {

    private final GraphSnapshot first = TestSnapshots.empty(TestSnapshots.END);
    private final GraphSnapshot second = TestSnapshots.empty(TestSnapshots.END.plusSeconds(60));
    private final GraphSnapshot third = TestSnapshots.empty(TestSnapshots.END.plusSeconds(120));

    @Test
    void dropsTheOldestSnapshotWhenFullAndCountsIt() throws InterruptedException {
        SnapshotQueue sut = new SnapshotQueue(2);

        sut.offer(first);
        sut.offer(second);
        sut.offer(third);

        assertThat(sut.size()).isEqualTo(2);
        assertThat(sut.dropped()).isEqualTo(1);
        assertThat(sut.poll(Duration.ofMillis(10))).isSameAs(second);
        assertThat(sut.poll(Duration.ofMillis(10))).isSameAs(third);
        assertThat(sut.poll(Duration.ofMillis(10))).isNull();
    }

    @Test
    void requeuePutsASnapshotBackAtTheFront() throws InterruptedException {
        SnapshotQueue sut = new SnapshotQueue(2);
        sut.offer(second);

        sut.requeue(first);

        assertThat(sut.dropped()).isZero();
        assertThat(sut.poll(Duration.ofMillis(10))).isSameAs(first);
        assertThat(sut.poll(Duration.ofMillis(10))).isSameAs(second);
    }

    @Test
    void requeueDropsTheSnapshotItselfWhenTheQueueIsFull() throws InterruptedException {
        SnapshotQueue sut = new SnapshotQueue(2);
        sut.offer(second);
        sut.offer(third);

        sut.requeue(first);

        assertThat(sut.dropped()).isEqualTo(1);
        assertThat(sut.poll(Duration.ofMillis(10))).isSameAs(second);
        assertThat(sut.poll(Duration.ofMillis(10))).isSameAs(third);
    }
}
