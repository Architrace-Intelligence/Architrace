/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology;

public class ScopeNotFoundException extends RuntimeException {

  public ScopeNotFoundException(Scope scope) {
    super(
        "no agent has registered for scope "
            + scope.project()
            + "/"
            + scope.environment()
            + "/"
            + scope.cluster());
  }
}
