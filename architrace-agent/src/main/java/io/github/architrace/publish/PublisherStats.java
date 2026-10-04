/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.publish;

import java.util.concurrent.atomic.LongAdder;

public final class PublisherStats {

    private final LongAdder published = new LongAdder();
    private final LongAdder acknowledged = new LongAdder();
    private final LongAdder rejected = new LongAdder();
    private final LongAdder sessions = new LongAdder();

    public void published() {
        published.increment();
    }

    public void acknowledged() {
        acknowledged.increment();
    }

    public void rejected() {
        rejected.increment();
    }

    public void sessionEnded() {
        sessions.increment();
    }

    public long publishedCount() {
        return published.sum();
    }

    public long acknowledgedCount() {
        return acknowledged.sum();
    }

    public long rejectedCount() {
        return rejected.sum();
    }

    public long endedSessions() {
        return sessions.sum();
    }
}
