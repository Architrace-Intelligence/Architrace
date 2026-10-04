/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.otlp;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.pipeline.SpanQueue;
import io.github.architrace.span.AttributeMapping;
import io.github.architrace.span.SpanNormaliser;
import io.github.architrace.testsupport.OtlpRequests;
import io.github.architrace.testsupport.RecordingObserver;
import io.github.architrace.testsupport.TestDataProvider;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.trace.v1.Span;
import org.junit.jupiter.api.Test;

class OtlpTraceServiceImplTest {

    private final SpanQueue queue = new SpanQueue(16);
    private final OtlpTraceServiceImpl sut = new OtlpTraceServiceImpl(
            new SpanReceiver(new SpanNormaliser(AttributeMapping.defaults(), "DEV", "cluster-1"), queue));

    @Test
    void exportShouldRespondCompleteAndQueueTheNormalisedSpans() {
        RecordingObserver<ExportTraceServiceResponse> responseObserver = new RecordingObserver<>();
        var request = OtlpRequests.request(
                OtlpRequests.standardResource("checkout"),
                OtlpRequests.span(Span.SpanKind.SPAN_KIND_SERVER, OtlpRequests.TRACE_ID, OtlpRequests.SERVER_SPAN_ID));

        sut.export(request, responseObserver);

        assertThat(responseObserver.values()).containsExactly(ExportTraceServiceResponse.getDefaultInstance());
        assertThat(responseObserver.isCompleted()).isTrue();
        assertThat(queue.size()).isEqualTo(1);
    }

    @Test
    void exportShouldAcceptSpansWithoutAService() {
        RecordingObserver<ExportTraceServiceResponse> responseObserver = new RecordingObserver<>();

        sut.export(TestDataProvider.createSingleSpanRequest("unit-test-span"), responseObserver);

        assertThat(responseObserver.isCompleted()).isTrue();
        assertThat(queue.size()).isZero();
    }
}
