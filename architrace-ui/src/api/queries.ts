/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { queryOptions } from "@tanstack/react-query";
import { getGraph, listScopes, type Scope } from "./client";

export function scopesQuery() {
  return queryOptions({
    queryKey: ["scopes"],
    queryFn: ({ signal }) => listScopes(signal),
  });
}

export function graphQuery(scope: Scope, at?: string) {
  return queryOptions({
    queryKey: ["graph", scope.project, scope.environment, scope.cluster, at ?? "live"],
    queryFn: ({ signal }) => getGraph(scope, at, signal),
  });
}
