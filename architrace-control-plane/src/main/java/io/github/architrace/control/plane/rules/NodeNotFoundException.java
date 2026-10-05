/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import io.github.architrace.control.plane.topology.Scope;

public class NodeNotFoundException extends RuntimeException {

    public NodeNotFoundException(Scope scope, String nodeId) {
        super("no node " + nodeId + " in the graph of scope " + scope.project() + "/" + scope.environment() + "/"
                + scope.cluster());
    }
}
