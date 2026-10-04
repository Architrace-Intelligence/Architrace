/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.otlp;

import io.github.architrace.span.SpanRecord;

public class SpanRingBuffer {

    private final SpanRecord[] buffer;
    private final int mask;
    private volatile long writeSeq = 0;
    private volatile long readSeq = 0;

    public SpanRingBuffer(int sizePowerOfTwo) {
        if (Integer.bitCount(sizePowerOfTwo) != 1) {
            throw new IllegalArgumentException("size must be power of two");
        }
        this.buffer = new SpanRecord[sizePowerOfTwo];
        this.mask = sizePowerOfTwo - 1;
    }

    public boolean publish(SpanRecord span) {
        long next = writeSeq + 1;
        if (next - readSeq > buffer.length) {
            return false;
        }
        buffer[(int) (writeSeq & mask)] = span;
        writeSeq = next;
        return true;
    }

    public SpanRecord poll() {
        if (readSeq >= writeSeq) {
            return null;
        }
        SpanRecord span = buffer[(int) (readSeq & mask)];
        buffer[(int) (readSeq & mask)] = null;
        readSeq++;
        return span;
    }
}
