/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.ingestion;

public class InvalidSnapshotException extends RuntimeException {

  public InvalidSnapshotException(String message) {
    super(message);
  }

  public InvalidSnapshotException(String message, Throwable cause) {
    super(message, cause);
  }
}
