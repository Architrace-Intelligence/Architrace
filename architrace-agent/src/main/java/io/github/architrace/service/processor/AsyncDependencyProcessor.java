/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.service.processor;

import io.github.architrace.model.InternalSpan;
import io.github.architrace.model.SpanType;
import io.github.architrace.service.graph.AsyncDependencyResolver;

public class AsyncDependencyProcessor implements SpanProcessor {

  private final AsyncDependencyResolver resolver;

  public AsyncDependencyProcessor(AsyncDependencyResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public void onSpan(InternalSpan span) {
    if (span.spanType() == SpanType.ASYNC) {
      resolver.onSpan(span);
    }
  }
}