/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.service.processor;

import io.github.architrace.service.graph.AsyncDependencyResolver;
import io.github.architrace.span.SpanKind;
import io.github.architrace.span.SpanRecord;

public class AsyncDependencyProcessor implements SpanProcessor {

    private final AsyncDependencyResolver resolver;

    public AsyncDependencyProcessor(AsyncDependencyResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void onSpan(SpanRecord span) {
        if (span.kind() == SpanKind.PRODUCER || span.kind() == SpanKind.CONSUMER) {
            resolver.onSpan(span);
        }
    }
}
