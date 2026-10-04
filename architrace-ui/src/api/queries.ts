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

export function graphQuery(scope: Scope) {
  return queryOptions({
    queryKey: ["graph", scope.project, scope.environment, scope.cluster],
    queryFn: ({ signal }) => getGraph(scope, signal),
  });
}
