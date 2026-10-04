/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.service.processor;

import io.github.architrace.service.graph.NodeRegistry;
import io.github.architrace.span.SpanRecord;

public class NodeProcessor implements SpanProcessor {

    private final NodeRegistry nodeRegistry;

    public NodeProcessor(NodeRegistry nodeRegistry) {
        this.nodeRegistry = nodeRegistry;
    }

    @Override
    public void onSpan(SpanRecord span) {
        nodeRegistry.register(span);
    }
}
