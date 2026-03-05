/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.model;

public record LogicalServiceId(
    String environment,
    String domainId,
    String serviceName) {

  public String asString() {
    return environment + ":" + domainId + ":" + serviceName;
  }

  @Override
  public String toString() {
    return asString();
  }

}