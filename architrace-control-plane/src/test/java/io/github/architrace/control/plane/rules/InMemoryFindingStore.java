/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import io.github.architrace.control.plane.topology.Scope;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryFindingStore implements FindingStore {

    private final Map<Scope, List<Finding>> findings = new HashMap<>();

    @Override
    public void replace(Scope scope, List<Finding> findings) {
        this.findings.put(scope, List.copyOf(findings));
    }

    @Override
    public List<Finding> findings(Scope scope) {
        return findings.getOrDefault(scope, List.of()).stream()
                .sorted(Finding.ORDER)
                .toList();
    }
}
