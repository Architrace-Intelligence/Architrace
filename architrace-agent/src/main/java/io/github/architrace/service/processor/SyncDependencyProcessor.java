/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.service.processor;

import io.github.architrace.service.graph.SyncDependencyResolver;
import io.github.architrace.span.SpanKind;
import io.github.architrace.span.SpanRecord;

public class SyncDependencyProcessor implements SpanProcessor {

    private final SyncDependencyResolver resolver;

    public SyncDependencyProcessor(SyncDependencyResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void onSpan(SpanRecord span) {
        if (span.kind() == SpanKind.CLIENT || span.kind() == SpanKind.SERVER) {
            resolver.onSpan(span);
        }
    }
}
