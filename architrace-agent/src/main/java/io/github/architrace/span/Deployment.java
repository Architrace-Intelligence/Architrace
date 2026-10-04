/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.span;

import java.util.Objects;
import java.util.Optional;

public record Deployment(String cluster, Optional<String> namespace, Optional<String> instance) {

    public Deployment {
        Objects.requireNonNull(cluster, "cluster");
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(instance, "instance");
    }
}
