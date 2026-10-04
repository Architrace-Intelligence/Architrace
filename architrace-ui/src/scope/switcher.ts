/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import type { Scope } from "../api/client";

export type ScopeLevel = "project" | "environment" | "cluster";

export const SCOPE_LEVELS: readonly ScopeLevel[] = ["project", "environment", "cluster"];

export function scopeOptions(
  scopes: readonly Scope[],
  current: Scope,
  level: ScopeLevel,
): string[] {
  const values = scopes
    .filter((scope) => sharesLevelsAbove(scope, current, level))
    .map((scope) => scope[level]);
  return [...new Set(values)].sort((left, right) => left.localeCompare(right));
}

export function switchScope(
  scopes: readonly Scope[],
  current: Scope,
  level: ScopeLevel,
  value: string,
): Scope | undefined {
  const candidates = scopes
    .filter((scope) => sharesLevelsAbove(scope, current, level) && scope[level] === value)
    .sort(
      (left, right) =>
        left.environment.localeCompare(right.environment) ||
        left.cluster.localeCompare(right.cluster),
    );
  const preferences: readonly ((scope: Scope) => boolean)[] = [
    (scope) => scope.environment === current.environment && scope.cluster === current.cluster,
    (scope) => scope.environment === current.environment,
    (scope) => scope.cluster === current.cluster,
  ];
  return (
    preferences
      .map((preferred) => candidates.find(preferred))
      .find((candidate) => candidate !== undefined) ?? candidates[0]
  );
}

function sharesLevelsAbove(scope: Scope, current: Scope, level: ScopeLevel): boolean {
  return SCOPE_LEVELS.slice(0, SCOPE_LEVELS.indexOf(level)).every(
    (key) => scope[key] === current[key],
  );
}
