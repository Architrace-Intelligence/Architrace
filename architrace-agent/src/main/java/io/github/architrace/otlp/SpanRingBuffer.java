/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.otlp;

import io.github.architrace.model.InternalSpan;

public class SpanRingBuffer {

  private final InternalSpan[] buffer;
  private final int mask;

  private volatile long writeSeq = 0;
  private volatile long readSeq = 0;

  public SpanRingBuffer(int sizePowerOfTwo) {
    if (Integer.bitCount(sizePowerOfTwo) != 1) {
      throw new IllegalArgumentException("size must be power of two");
    }

    this.buffer = new InternalSpan[sizePowerOfTwo];
    this.mask = sizePowerOfTwo - 1;
  }

  public boolean publish(InternalSpan span) {
    long next = writeSeq + 1;

    if (next - readSeq > buffer.length) {
      return false; // buffer full
    }

    buffer[(int) (writeSeq & mask)] = span;
    writeSeq = next;

    return true;
  }

  public InternalSpan poll() {
    if (readSeq >= writeSeq) {
      return null;
    }

    InternalSpan span = buffer[(int) (readSeq & mask)];
    buffer[(int) (readSeq & mask)] = null;

    readSeq++;

    return span;
  }
}