/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.web;

import io.github.architrace.control.plane.topology.InvalidQueryException;
import io.github.architrace.control.plane.topology.ScopeNotFoundException;
import io.github.architrace.control.plane.topology.SnapshotNotFoundException;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ProblemDetailsAdvice {

  private static final String TYPE_PREFIX = "urn:architrace:problem:";

  @ExceptionHandler
  ProblemDetail scopeNotFound(ScopeNotFoundException e) {
    return problem(HttpStatus.NOT_FOUND, "scope-not-found", "Scope not found", e);
  }

  @ExceptionHandler
  ProblemDetail snapshotNotFound(SnapshotNotFoundException e) {
    return problem(HttpStatus.NOT_FOUND, "snapshot-not-found", "Snapshot not found", e);
  }

  @ExceptionHandler
  ProblemDetail invalidQuery(InvalidQueryException e) {
    return problem(HttpStatus.BAD_REQUEST, "invalid-query", "Invalid query", e);
  }

  private static ProblemDetail problem(
      HttpStatus status, String slug, String title, RuntimeException cause) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, cause.getMessage());
    problem.setType(URI.create(TYPE_PREFIX + slug));
    problem.setTitle(title);
    return problem;
  }
}
