/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.outbound;

public enum OverflowStrategy {
  DROP_LATEST,
  DROP_OLDEST,
  BLOCK
}
