/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.service.runtime;

import io.github.architrace.model.InternalSpan;
import io.github.architrace.service.processor.SpanProcessor;

public class SamplingProcessor implements SpanProcessor {

  @Override
  public void onSpan(InternalSpan spans) {
  }
}
