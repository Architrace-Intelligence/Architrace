/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import java.util.Objects;

public record AgentRegistration(String name, String version, Scope scope) {

    public AgentRegistration {
        Names.requireIdentifier(name, "name");
        version = version == null ? "" : version;
        Objects.requireNonNull(scope, "scope");
    }
}
