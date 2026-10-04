/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.otlp;

import io.github.architrace.model.LogicalServiceId;
import io.github.architrace.model.NodeType;
import io.github.architrace.span.Peer;
import io.github.architrace.span.SpanKind;
import io.github.architrace.span.SpanRecord;

public class NodeExtractor {

    public NodeDescriptor extract(SpanRecord span) {
        return switch (span.peer()) {
            case Peer.Database database ->
                new NodeDescriptor("db:" + database.system(), NodeType.DATABASE, database.system());
            case Peer.Messaging messaging ->
                messaging
                        .destination()
                        .map(topic -> new NodeDescriptor("topic:" + topic, NodeType.TOPIC, topic))
                        .orElseGet(() -> serviceNode(span));
            case Peer.Http http ->
                span.kind() == SpanKind.CLIENT
                        ? new NodeDescriptor("ext:" + http.address(), NodeType.EXTERNAL_SERVICE, http.address())
                        : serviceNode(span);
            case Peer.None _ -> serviceNode(span);
        };
    }

    private static NodeDescriptor serviceNode(SpanRecord span) {
        LogicalServiceId id = LogicalServiceId.of(span.service());
        return new NodeDescriptor(id.asString(), NodeType.SERVICE, id.serviceName());
    }
}
