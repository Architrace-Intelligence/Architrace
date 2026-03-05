/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.service.processor;

import io.github.architrace.model.InternalSpan;
import io.github.architrace.model.SpanType;
import io.github.architrace.service.graph.SyncDependencyResolver;

public class SyncDependencyProcessor implements SpanProcessor {

  private final SyncDependencyResolver resolver;

  public SyncDependencyProcessor(SyncDependencyResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public void onSpan(InternalSpan span) {
    if(span.spanType() == SpanType.SYNC){
      resolver.onSpan(span);
    }
  }

}