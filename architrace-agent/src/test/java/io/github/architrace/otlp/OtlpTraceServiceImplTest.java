/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.otlp;


import io.github.architrace.service.graph.SpanExtractor;
import io.github.architrace.testsupport.TestDataProvider;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OtlpTraceServiceImplTest {

  private OtlpTraceServiceImpl sut;

  @BeforeEach
  void setUp() {
//    sut = new OtlpTraceServiceImpl(new SpanExtractor());
  }

  @Test
  void exportShouldRespondAndComplete() {
    RecordingObserver responseObserver = new RecordingObserver();

    var request = TestDataProvider.createSingleSpanRequest("unit-test-span");

    sut.export(request, responseObserver);

    assertThat(responseObserver.values).hasSize(1);
    assertThat(responseObserver.values.get(0)).isEqualTo(ExportTraceServiceResponse.getDefaultInstance());
    assertThat(responseObserver.completed).isTrue();
  }

  private static final class RecordingObserver implements StreamObserver<ExportTraceServiceResponse> {
    private final List<ExportTraceServiceResponse> values = new ArrayList<>();
    private boolean completed;

    @Override
    public void onNext(ExportTraceServiceResponse value) {
      values.add(value);
    }

    @Override
    public void onError(Throwable throwable) {
      // No-op for tests; this scenario verifies happy-path export only.
    }

    @Override
    public void onCompleted() {
      completed = true;
    }
  }
}
