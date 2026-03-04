/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.model;

public enum SpanKind {
  CLIENT,
  SERVER,
  INTERNAL,
  PRODUCER,
  CONSUMER,
  UNSPECIFIED;
}
