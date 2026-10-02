/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { useQuery } from "@tanstack/react-query";
import { ProblemError } from "../api/client";
import { scopesQuery } from "../api/queries";

export function ControlPlaneStatus() {
  const scopes = useQuery(scopesQuery());

  if (scopes.isPending) {
    return <output className="status">Connecting to the control plane…</output>;
  }
  if (scopes.isError) {
    return (
      <p className="status status-error" role="alert">
        The control plane did not answer: {describe(scopes.error)}
      </p>
    );
  }
  return (
    <output className="status">{formatScopeCount(scopes.data.length)} reported by agents.</output>
  );
}

function describe(error: Error): string {
  return error instanceof ProblemError ? (error.problem.detail ?? error.message) : error.message;
}

function formatScopeCount(count: number): string {
  return count === 1 ? "1 scope" : `${String(count)} scopes`;
}
