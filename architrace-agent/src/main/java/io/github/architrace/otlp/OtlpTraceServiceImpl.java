/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.otlp;

import io.github.architrace.graph.ControlPlanePublisher;
import io.github.architrace.model.GraphAggregator;
import io.github.architrace.graph.SpanToGraphConverter;
import io.github.architrace.model.InternalSpan;
import io.grpc.stub.StreamObserver;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OtlpTraceServiceImpl extends TraceServiceGrpc.TraceServiceImplBase {

  private static final Logger log = LoggerFactory.getLogger(OtlpTraceServiceImpl.class);
  private final String agentName;
  private final SpanToGraphConverter converter;
  private final GraphAggregator aggregator;
  private final ControlPlanePublisher publisher;

  public OtlpTraceServiceImpl(
      String agentName,
      SpanToGraphConverter converter,
      GraphAggregator aggregator,
      ControlPlanePublisher publisher) {
    this.agentName = agentName;
    this.converter = converter;
    this.aggregator = aggregator;
    this.publisher = publisher;
  }

  @Override
  public void export(
      ExportTraceServiceRequest request,
      StreamObserver<ExportTraceServiceResponse> responseObserver) {
    long spanCount = 0L;
    for (var resourceSpans : request.getResourceSpansList()) {
      for (var scopeSpans : resourceSpans.getScopeSpansList()) {
        for (var span : scopeSpans.getSpansList()) {
          log.info("span {}", span);
          spanCount++;
        }
      }
    }

    List<InternalSpan> extract = converter.extract(request);
//    aggregator.drain().ifPresent(publisher::publish);

    log.info("OTLP export received on receiver: {} span(s).", spanCount);
    responseObserver.onNext(ExportTraceServiceResponse.newBuilder().build());
    responseObserver.onCompleted();
  }
}
