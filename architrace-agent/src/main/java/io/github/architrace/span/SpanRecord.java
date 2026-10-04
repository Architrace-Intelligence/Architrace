/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.span;

import java.util.Objects;
import java.util.Optional;

public record SpanRecord(
        String traceId,
        String spanId,
        Optional<String> parentSpanId,
        SpanKind kind,
        long startEpochNanos,
        long endEpochNanos,
        boolean error,
        ServiceIdentity service,
        Deployment deployment,
        Peer peer) {

    private static final long NANOS_PER_MILLI = 1_000_000L;

    public SpanRecord {
        Objects.requireNonNull(traceId, "traceId");
        Objects.requireNonNull(spanId, "spanId");
        Objects.requireNonNull(parentSpanId, "parentSpanId");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(service, "service");
        Objects.requireNonNull(deployment, "deployment");
        Objects.requireNonNull(peer, "peer");
    }

    public long latencyMillis() {
        return Math.max(0L, (endEpochNanos - startEpochNanos) / NANOS_PER_MILLI);
    }
}
