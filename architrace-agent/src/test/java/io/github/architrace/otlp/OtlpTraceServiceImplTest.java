/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.otlp;


import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.service.graph.SpanExtractor;
import io.github.architrace.service.processor.SpanBatchProcessor;
import io.github.architrace.testsupport.RecordingObserver;
import io.github.architrace.testsupport.TestDataProvider;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OtlpTraceServiceImplTest {

  private OtlpTraceServiceImpl sut;

  @BeforeEach
  void setUp() {
    var batchProcessor = new SpanBatchProcessor(new SpanRingBuffer(16), new SpanPipeline(List.of()));
    sut = new OtlpTraceServiceImpl(new SpanReceiver(new SpanExtractor(), batchProcessor));
  }

  @Test
  void exportShouldRespondAndComplete() {
    RecordingObserver<ExportTraceServiceResponse> responseObserver = new RecordingObserver<>();

    var request = TestDataProvider.createSingleSpanRequest("unit-test-span");

    sut.export(request, responseObserver);

    assertThat(responseObserver.values()).hasSize(1);
    assertThat(responseObserver.values().get(0)).isEqualTo(ExportTraceServiceResponse.getDefaultInstance());
    assertThat(responseObserver.isCompleted()).isTrue();
  }
}
