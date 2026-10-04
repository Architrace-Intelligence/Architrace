/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.service.graph;

import io.github.architrace.model.EdgeKey;
import io.github.architrace.model.EdgeMetrics;
import io.github.architrace.model.LogicalServiceId;
import io.github.architrace.span.SpanRecord;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractDependencyResolver {

    private final ConcurrentMap<EdgeKey, EdgeMetrics> edges = new ConcurrentHashMap<>();

    protected final void buildDependency(SpanRecord fromSpan, SpanRecord toSpan) {
        LogicalServiceId from = LogicalServiceId.of(fromSpan.service());
        LogicalServiceId to = LogicalServiceId.of(toSpan.service());
        if (!from.environment().equals(to.environment()) || from.equals(to)) {
            return;
        }
        edges.computeIfAbsent(new EdgeKey(from.asString(), to.asString()), _ -> new EdgeMetrics());
    }

    public ConcurrentMap<EdgeKey, EdgeMetrics> getEdges() {
        return edges;
    }
}
