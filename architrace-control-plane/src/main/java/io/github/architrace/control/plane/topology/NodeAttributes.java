/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology;

import java.util.Map;
import java.util.Set;

public record NodeAttributes(
    Set<String> versions, Set<Deployment> deployments, Map<String, String> labels) {

  private static final NodeAttributes NONE = new NodeAttributes(Set.of(), Set.of(), Map.of());

  public NodeAttributes {
    versions = Set.copyOf(versions);
    deployments = Set.copyOf(deployments);
    labels = Map.copyOf(labels);
  }

  public static NodeAttributes none() {
    return NONE;
  }
}
