/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import type { ScopeSummary } from "../api/client";

export type GroupBy = "project" | "environment" | "cluster";
export type FacetKey = "environment" | "cluster";

export const GROUP_BY_OPTIONS: readonly GroupBy[] = ["project", "environment", "cluster"];
export const DEFAULT_GROUP_BY: GroupBy = "project";

export interface ProjectsFilter {
  readonly environments: readonly string[];
  readonly clusters: readonly string[];
  readonly query: string;
  readonly groupBy: GroupBy;
}

export interface FacetValue {
  readonly value: string;
  readonly count: number;
  readonly selected: boolean;
}

export interface ScopeGroup {
  readonly key: string;
  readonly scopes: readonly ScopeSummary[];
  readonly projects: number;
  readonly environments: number;
  readonly clusters: number;
  readonly services: number;
  readonly dataStreams: number;
}

export interface Summary {
  readonly total: number;
  readonly visible: number;
  readonly projects: number;
  readonly environments: number;
  readonly clusters: number;
  readonly agents: number;
  readonly staleAgents: number;
}

export const EMPTY_FILTER: ProjectsFilter = {
  environments: [],
  clusters: [],
  query: "",
  groupBy: DEFAULT_GROUP_BY,
};

function isGroupBy(value: string): value is GroupBy {
  return (GROUP_BY_OPTIONS as readonly string[]).includes(value);
}

export function parseFilter(params: URLSearchParams): ProjectsFilter {
  const group = params.get("group") ?? DEFAULT_GROUP_BY;
  return {
    environments: distinct(params.getAll("env")),
    clusters: distinct(params.getAll("cluster")),
    query: params.get("q") ?? "",
    groupBy: isGroupBy(group) ? group : DEFAULT_GROUP_BY,
  };
}

export function toParams(filter: ProjectsFilter): URLSearchParams {
  const params = new URLSearchParams();
  filter.environments.forEach((value) => {
    params.append("env", value);
  });
  filter.clusters.forEach((value) => {
    params.append("cluster", value);
  });
  if (filter.query !== "") {
    params.set("q", filter.query);
  }
  if (filter.groupBy !== DEFAULT_GROUP_BY) {
    params.set("group", filter.groupBy);
  }
  return params;
}

export function isEmptyFilter(filter: ProjectsFilter): boolean {
  return filter.environments.length === 0 && filter.clusters.length === 0 && filter.query === "";
}

export function toggleFacetValue(
  filter: ProjectsFilter,
  key: FacetKey,
  value: string,
): ProjectsFilter {
  const current = key === "environment" ? filter.environments : filter.clusters;
  const next = current.includes(value)
    ? current.filter((candidate) => candidate !== value)
    : [...current, value];
  return key === "environment" ? { ...filter, environments: next } : { ...filter, clusters: next };
}

export function applyFilter(
  scopes: readonly ScopeSummary[],
  filter: ProjectsFilter,
): ScopeSummary[] {
  return scopes.filter(
    (scope) =>
      matchesFacet(filter.environments, scope.scope.environment) &&
      matchesFacet(filter.clusters, scope.scope.cluster) &&
      matchesQuery(filter.query, scope),
  );
}

export function facetValues(
  scopes: readonly ScopeSummary[],
  filter: ProjectsFilter,
  key: FacetKey,
): FacetValue[] {
  const selected = key === "environment" ? filter.environments : filter.clusters;
  const others: ProjectsFilter =
    key === "environment" ? { ...filter, environments: [] } : { ...filter, clusters: [] };
  const counts = new Map<string, number>();
  scopes.forEach((scope) => counts.set(scope.scope[key], 0));
  applyFilter(scopes, others).forEach((scope) =>
    counts.set(scope.scope[key], (counts.get(scope.scope[key]) ?? 0) + 1),
  );
  return [...counts.entries()]
    .map(([value, count]) => ({ value, count, selected: selected.includes(value) }))
    .sort((left, right) => left.value.localeCompare(right.value));
}

export function groupScopes(scopes: readonly ScopeSummary[], groupBy: GroupBy): ScopeGroup[] {
  const byKey = new Map<string, ScopeSummary[]>();
  scopes.forEach((scope) => {
    const key = scope.scope[groupBy];
    byKey.set(key, [...(byKey.get(key) ?? []), scope]);
  });
  return [...byKey.entries()]
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([key, members]) => ({
      key,
      scopes: sortScopes(members),
      projects: distinct(members.map((scope) => scope.scope.project)).length,
      environments: distinct(members.map((scope) => scope.scope.environment)).length,
      clusters: distinct(members.map((scope) => scope.scope.cluster)).length,
      services: sum(members.map((scope) => scope.services)),
      dataStreams: sum(members.map((scope) => scope.dataStreams)),
    }));
}

export function summarise(all: readonly ScopeSummary[], visible: readonly ScopeSummary[]): Summary {
  return {
    total: all.length,
    visible: visible.length,
    projects: distinct(visible.map((scope) => scope.scope.project)).length,
    environments: distinct(visible.map((scope) => scope.scope.environment)).length,
    clusters: distinct(visible.map((scope) => scope.scope.cluster)).length,
    agents: sum(visible.map((scope) => scope.agents)),
    staleAgents: sum(visible.map((scope) => scope.agents - scope.liveAgents)),
  };
}

export function scopePath(scope: ScopeSummary["scope"]): string {
  return ["/scopes", scope.project, scope.environment, scope.cluster]
    .map((segment, index) => (index === 0 ? segment : encodeURIComponent(segment)))
    .join("/");
}

function matchesFacet(selected: readonly string[], value: string): boolean {
  return selected.length === 0 || selected.includes(value);
}

function matchesQuery(query: string, scope: ScopeSummary): boolean {
  const needle = query.trim().toLowerCase();
  return (
    needle === "" ||
    [scope.scope.project, scope.scope.environment, scope.scope.cluster].some((value) =>
      value.toLowerCase().includes(needle),
    )
  );
}

function sortScopes(scopes: readonly ScopeSummary[]): ScopeSummary[] {
  return [...scopes].sort(
    (left, right) =>
      left.scope.project.localeCompare(right.scope.project) ||
      left.scope.environment.localeCompare(right.scope.environment) ||
      left.scope.cluster.localeCompare(right.scope.cluster),
  );
}

function distinct(values: readonly string[]): string[] {
  return [...new Set(values)];
}

function sum(values: readonly number[]): number {
  return values.reduce((total, value) => total + value, 0);
}
