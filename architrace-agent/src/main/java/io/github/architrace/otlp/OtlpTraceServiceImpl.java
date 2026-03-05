/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.otlp;

import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OtlpTraceServiceImpl extends TraceServiceGrpc.TraceServiceImplBase {

  private static final Logger log = LoggerFactory.getLogger(OtlpTraceServiceImpl.class);
  private final SpanReceiver receiver;

  public OtlpTraceServiceImpl(SpanReceiver receiver) {
    this.receiver = receiver;
  }

  @Override
  public void export(ExportTraceServiceRequest request, StreamObserver<ExportTraceServiceResponse> responseObserver) {
    receiver.receive(request);

    responseObserver.onNext(ExportTraceServiceResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}
