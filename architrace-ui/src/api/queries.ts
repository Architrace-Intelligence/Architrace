/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { queryOptions } from "@tanstack/react-query";
import {
  getEnvironmentDiff,
  getGraph,
  getTimelineDiff,
  listFindings,
  listScopes,
  type Scope,
} from "./client";

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

export function environmentDiffQuery(
  right: Scope,
  leftEnvironment: string,
  leftCluster: string,
  at?: string,
) {
  return queryOptions({
    queryKey: [
      "diff",
      "environments",
      right.project,
      right.environment,
      right.cluster,
      leftEnvironment,
      leftCluster,
      at ?? "live",
    ],
    queryFn: ({ signal }) => getEnvironmentDiff(right, leftEnvironment, leftCluster, at, signal),
  });
}

export function timelineDiffQuery(scope: Scope, from: string, to?: string) {
  return queryOptions({
    queryKey: [
      "diff",
      "timeline",
      scope.project,
      scope.environment,
      scope.cluster,
      from,
      to ?? "live",
    ],
    queryFn: ({ signal }) => getTimelineDiff(scope, from, to, signal),
  });
}

export function findingsQuery(scope: Scope) {
  return queryOptions({
    queryKey: ["findings", scope.project, scope.environment, scope.cluster],
    queryFn: ({ signal }) => listFindings(scope, signal),
  });
}
