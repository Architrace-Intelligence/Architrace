/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.model;

public enum NodeType {
  SERVICE,
  DATABASE,
  MESSAGE_BROKER,
  EXTERNAL_SERVICE,
  QUEUE,
  TOPIC
}
