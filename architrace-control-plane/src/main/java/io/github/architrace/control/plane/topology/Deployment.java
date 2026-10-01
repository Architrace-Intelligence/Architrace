/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology;

public record Deployment(String cluster, String namespace) {

  public Deployment {
    Names.requireIdentifier(cluster, "cluster");
    namespace = namespace == null ? "" : namespace;
  }
}
