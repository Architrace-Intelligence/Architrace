/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology.persistence;

import java.util.Objects;

public record JsonDocument(String json) {

    public JsonDocument {
        Objects.requireNonNull(json, "json");
    }
}
