/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.service.graph;

import io.github.architrace.model.InternalSpan;
import io.github.architrace.model.AsyncKey;
import io.github.architrace.model.SpanKind;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AsyncDependencyResolver extends AbstractDependencyResolver {

  private final ConcurrentMap<AsyncKey, InternalSpan> waitingProducers = new ConcurrentHashMap<>();

  public void onSpan(InternalSpan span) {
    if (span.kind() == SpanKind.PRODUCER) {
      handleProducer(span);
    }

    if (span.kind() == SpanKind.CONSUMER) {
      handleConsumer(span);
    }
  }

  private void handleProducer(InternalSpan producer) {
    String destination = producer.messagingDestination();
    if (destination == null) {
      return;
    }

    AsyncKey key = new AsyncKey(producer.traceId(), destination);
    waitingProducers.put(key, producer);
  }

  private void handleConsumer(InternalSpan consumer) {
    String destination = consumer.messagingDestination();
    if (destination == null) {
      return;
    }

    AsyncKey key = new AsyncKey(consumer.traceId(), destination);
    InternalSpan producer = waitingProducers.remove(key);
    if (producer == null) {
      return;
    }

    buildDependency(producer, consumer);
  }
}
