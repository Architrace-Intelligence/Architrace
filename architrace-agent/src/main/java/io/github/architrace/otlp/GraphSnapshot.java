/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.otlp;

import io.github.architrace.model.EdgeKey;
import io.github.architrace.model.EdgeMetrics;
import java.util.Map;
import java.util.Set;

public record GraphSnapshot(
    Set<GraphNode> nodes,
    Map<EdgeKey, EdgeMetrics> edges
) {}