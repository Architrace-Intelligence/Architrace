/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.testsupport;

import io.github.architrace.span.Deployment;
import io.github.architrace.span.Peer;
import io.github.architrace.span.ServiceIdentity;
import io.github.architrace.span.SpanKind;
import io.github.architrace.span.SpanRecord;
import java.util.Optional;

public final class TestSpans {

    public static final String ENVIRONMENT = "DEV";
    public static final String CLUSTER = "cluster-1";

    private TestSpans() {}

    public static SpanRecord span(SpanKind kind, String traceId, String spanId, String serviceName) {
        return span(kind, traceId, spanId, Optional.empty(), serviceName, new Peer.None(), ENVIRONMENT);
    }

    public static SpanRecord span(
            SpanKind kind,
            String traceId,
            String spanId,
            Optional<String> parentSpanId,
            String serviceName,
            Peer peer,
            String environment) {
        return new SpanRecord(
                traceId,
                spanId,
                parentSpanId,
                kind,
                1_000_000_000L,
                1_250_000_000L,
                false,
                new ServiceIdentity(environment, "default", serviceName, "1.0"),
                new Deployment(CLUSTER, Optional.empty(), Optional.empty()),
                peer);
    }

    public static SpanRecord child(SpanKind kind, String traceId, String spanId, String parentSpanId, String service) {
        return span(kind, traceId, spanId, Optional.of(parentSpanId), service, new Peer.None(), ENVIRONMENT);
    }

    public static SpanRecord withPeer(SpanKind kind, String traceId, String spanId, String service, Peer peer) {
        return span(kind, traceId, spanId, Optional.empty(), service, peer, ENVIRONMENT);
    }
}
