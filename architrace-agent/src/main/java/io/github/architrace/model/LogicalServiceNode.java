/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.model;

import java.util.Collections;
import java.util.Set;

public record LogicalServiceNode(
    LogicalServiceId logicalServiceId,
    Set<String> clusters,
    Set<String> namespaces) implements GraphNode {

  @Override
  public String id() {
    return logicalServiceId.asString();
  }

  public Set<String> clusters() {
    return Collections.unmodifiableSet(clusters);
  }

  public Set<String> namespaces() {
    return Collections.unmodifiableSet(namespaces);
  }

  public void registerDeployment(String clusterId, String namespace) {
    if (clusterId != null) {
      clusters.add(clusterId);
    }
    if (namespace != null) {
      namespaces.add(namespace);
    }
  }

}