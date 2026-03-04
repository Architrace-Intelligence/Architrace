/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.model;

public class EdgeMetrics {

  private long callCount;
  private long errorCount;
  private long totalLatency;

  public void incrementCallCount() {
    this.callCount = this.callCount + 1;
  }
}
