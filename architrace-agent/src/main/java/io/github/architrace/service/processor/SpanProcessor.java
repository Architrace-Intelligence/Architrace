/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.service.processor;

import io.github.architrace.model.InternalSpan;

public interface SpanProcessor {

  void onSpan(InternalSpan spans);
}
