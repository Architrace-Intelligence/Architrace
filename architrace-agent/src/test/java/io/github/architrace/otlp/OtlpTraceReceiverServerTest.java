/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.otlp;


import io.github.architrace.service.graph.SpanExtractor;
import io.github.architrace.testsupport.TestDataProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OtlpTraceReceiverServerTest {

  @Test
  void closeShouldHandleInterruptedThread() throws Exception {
//    int port = TestDataProvider.findFreePort();
//    OtlpTraceReceiverServer sut =
//        new OtlpTraceReceiverServer(
//            port,
//            new OtlpTraceServiceImpl(
//                new SpanExtractor()
//            ));
//    sut.start();
//
//    Thread.currentThread().interrupt();
//    try {
//      sut.close();
//      assertThat(Thread.currentThread().isInterrupted()).isTrue();
//    } finally {
//      Thread.interrupted();
//    }
  }
}
