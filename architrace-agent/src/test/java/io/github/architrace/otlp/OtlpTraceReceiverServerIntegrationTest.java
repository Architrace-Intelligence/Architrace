/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.otlp;


import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.architrace.graph.ControlPlanePublisher;
import io.github.architrace.model.GraphAggregator;
import io.github.architrace.graph.SpanToGraphConverter;
import io.github.architrace.testsupport.TestDataProvider;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class OtlpTraceReceiverServerIntegrationTest {

  private static final String LOOPBACK_HOST = "127.0.0.1";
  private static final String TEST_SPAN_NAME = "integration-test-span";
  private static final long EXPORT_DEADLINE_SECONDS = 5L;
  private static final long CHANNEL_SHUTDOWN_TIMEOUT_SECONDS = 2L;
  private static final String EXPECTED_RECEIVER_LOG_MESSAGE =
      "OTLP export received on receiver: 1 span(s).";

  @Test
  void shouldReceiveAndLogOtlpTraceExport() throws Exception {
    int port = TestDataProvider.findFreePort();

    Logger logger = (Logger) LoggerFactory.getLogger(OtlpTraceServiceImpl.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    try (OtlpTraceReceiverServer sut =
             new OtlpTraceReceiverServer(
                 port,
                 new OtlpTraceServiceImpl(
                     "test-agent",
                     new SpanToGraphConverter(),
                     new GraphAggregator(),
                     new ControlPlanePublisher(() -> null)))) {
      sut.start();

      ManagedChannel channel =
          NettyChannelBuilder.forAddress(LOOPBACK_HOST, port).usePlaintext().build();
      try {
        var stub =
            TraceServiceGrpc.newBlockingStub(channel).withDeadlineAfter(EXPORT_DEADLINE_SECONDS, TimeUnit.SECONDS);
        var response = stub.export(TestDataProvider.createSingleSpanRequest(TEST_SPAN_NAME));
        assertThat(response).isNotNull();
      } finally {
        channel.shutdownNow();
        channel.awaitTermination(CHANNEL_SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
      }

      boolean logged =
          listAppender.list.stream()
              .map(ILoggingEvent::getFormattedMessage)
              .anyMatch(msg -> msg.contains(EXPECTED_RECEIVER_LOG_MESSAGE));

      assertThat(logged).isTrue();
    } finally {
      logger.detachAppender(listAppender);
    }
  }
}
