/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import io.github.architrace.control.plane.topology.Scope;
import java.util.List;
import java.util.Map;

public interface FindingStore {

    void replace(Scope scope, List<Finding> findings);

    List<Finding> findings(Scope scope);

    Map<Scope, FindingCounts> counts();
}
