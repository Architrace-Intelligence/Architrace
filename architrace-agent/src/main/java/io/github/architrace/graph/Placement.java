/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import io.github.architrace.span.Deployment;
import java.util.Objects;
import java.util.Optional;

public record Placement(String cluster, Optional<String> namespace) {

    public Placement {
        Objects.requireNonNull(cluster, "cluster");
        Objects.requireNonNull(namespace, "namespace");
    }

    public static Placement of(Deployment deployment) {
        return new Placement(deployment.cluster(), deployment.namespace());
    }
}
