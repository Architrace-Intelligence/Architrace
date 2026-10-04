/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.otlp;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.service.processor.SpanBatchProcessor;
import io.github.architrace.span.AttributeMapping;
import io.github.architrace.span.SpanNormaliser;
import io.github.architrace.testsupport.RecordingObserver;
import io.github.architrace.testsupport.TestDataProvider;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class OtlpTraceServiceImplTest {

    private final OtlpTraceServiceImpl sut = new OtlpTraceServiceImpl(new SpanReceiver(
            new SpanNormaliser(AttributeMapping.defaults(), "DEV", "cluster-1"),
            new SpanBatchProcessor(new SpanRingBuffer(16), new SpanPipeline(List.of()))));

    @Test
    void exportShouldRespondAndComplete() {
        RecordingObserver<ExportTraceServiceResponse> responseObserver = new RecordingObserver<>();
        var request = TestDataProvider.createSingleSpanRequest("unit-test-span");

        sut.export(request, responseObserver);

        assertThat(responseObserver.values()).containsExactly(ExportTraceServiceResponse.getDefaultInstance());
        assertThat(responseObserver.isCompleted()).isTrue();
    }
}
