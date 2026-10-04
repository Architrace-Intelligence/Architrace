/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import java.util.Objects;

public record EdgeKey(GraphNode source, GraphNode target, EdgeKind kind) {

    public EdgeKey {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(kind, "kind");
    }
}
