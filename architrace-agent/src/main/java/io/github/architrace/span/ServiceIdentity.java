/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.span;

import java.util.Objects;

public record ServiceIdentity(String environment, String domain, String name, String version) {

    public ServiceIdentity {
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(domain, "domain");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(version, "version");
    }
}
