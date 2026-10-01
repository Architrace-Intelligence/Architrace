/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology;

public class SnapshotNotFoundException extends RuntimeException {

  public SnapshotNotFoundException(SnapshotId id) {
    super("no snapshot with id " + id.value());
  }
}
