/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology;

import java.util.List;
import java.util.Objects;

public record Page<T>(List<T> items, PageRequest request, long totalItems) {

  public Page {
    items = List.copyOf(items);
    Objects.requireNonNull(request, "request");
    if (totalItems < 0) {
      throw new IllegalArgumentException("totalItems must not be negative: " + totalItems);
    }
  }

  public int totalPages() {
    return Math.toIntExact(Math.ceilDiv(totalItems, request.size()));
  }
}
