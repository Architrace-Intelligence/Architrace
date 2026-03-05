/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.otlp;

import io.github.architrace.model.NodeType;

public record NodeDescriptor(
    String id,
    NodeType type,
    String name
) {}