/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import java.util.Objects;

public record SnapshotEdge(EdgeKey key, EdgeMetricsSummary metrics) {

    public SnapshotEdge {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(metrics, "metrics");
    }
}
