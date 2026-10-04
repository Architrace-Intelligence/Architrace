/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import java.util.Objects;

record TraceSpanKey(String traceId, String spanId) {

    TraceSpanKey {
        Objects.requireNonNull(traceId, "traceId");
        Objects.requireNonNull(spanId, "spanId");
    }
}
