/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.span;

public enum SpanKind {
    CLIENT,
    SERVER,
    PRODUCER,
    CONSUMER,
    INTERNAL
}
