/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology;

public record PageRequest(int page, int size) {

  public static final int MAX_SIZE = 200;

  public PageRequest {
    if (page < 0) {
      throw new InvalidQueryException("page must not be negative: " + page);
    }
    if (size < 1 || size > MAX_SIZE) {
      throw new InvalidQueryException("size must be between 1 and " + MAX_SIZE + ": " + size);
    }
  }

  public long offset() {
    return (long) page * size;
  }
}
