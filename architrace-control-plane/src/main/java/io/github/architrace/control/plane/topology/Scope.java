/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology;

public record Scope(String project, String environment, String cluster) {

  public Scope {
    Names.requireIdentifier(project, "project");
    Names.requireIdentifier(environment, "environment");
    Names.requireIdentifier(cluster, "cluster");
  }
}
