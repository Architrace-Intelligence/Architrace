/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import createClient from "openapi-fetch";
import type { components, paths } from "./schema";

export type Problem = components["schemas"]["Problem"];
export type Scope = components["schemas"]["Scope"];
export type ScopeSummary = components["schemas"]["ScopeSummary"];
export type TopologyGraph = components["schemas"]["TopologyGraph"];
export type TopologyNode = components["schemas"]["TopologyNode"];
export type TopologyEdge = components["schemas"]["TopologyEdge"];
export type EdgeMetrics = components["schemas"]["EdgeMetrics"];
export type NodeType = components["schemas"]["NodeType"];
export type EdgeKind = components["schemas"]["EdgeKind"];
export type TopologyDiff = components["schemas"]["TopologyDiff"];
export type GraphRef = components["schemas"]["GraphRef"];
export type NodeChange = components["schemas"]["NodeChange"];
export type EdgeRef = components["schemas"]["EdgeRef"];

export class ProblemError extends Error {
  readonly problem: Problem;

  constructor(problem: Problem) {
    super(problem.title ?? `The control plane answered ${String(problem.status)}`);
    this.name = "ProblemError";
    this.problem = problem;
  }
}

export const client = createClient<paths>({
  baseUrl: new URL("/api/v1", document.baseURI).href,
  fetch: (request) => globalThis.fetch(request),
});

export async function listScopes(signal?: AbortSignal): Promise<ScopeSummary[]> {
  const { data, error, response } = await client.GET("/scopes", { signal });
  return unwrap(data, error, response);
}

export async function getGraph(
  scope: Scope,
  at: string | undefined,
  signal?: AbortSignal,
): Promise<TopologyGraph> {
  const { data, error, response } = await client.GET(
    "/scopes/{project}/{environment}/{cluster}/graph",
    { params: { path: scope, query: at === undefined ? undefined : { at } }, signal },
  );
  return unwrap(data, error, response);
}

export async function getEnvironmentDiff(
  right: Scope,
  leftEnvironment: string,
  leftCluster: string,
  at: string | undefined,
  signal?: AbortSignal,
): Promise<TopologyDiff> {
  const { data, error, response } = await client.GET(
    "/scopes/{project}/{environment}/{cluster}/diff/environments",
    {
      params: {
        path: right,
        query:
          at === undefined
            ? { leftEnvironment, leftCluster }
            : { leftEnvironment, leftCluster, at },
      },
      signal,
    },
  );
  return unwrap(data, error, response);
}

export async function getTimelineDiff(
  scope: Scope,
  from: string,
  to: string | undefined,
  signal?: AbortSignal,
): Promise<TopologyDiff> {
  const { data, error, response } = await client.GET(
    "/scopes/{project}/{environment}/{cluster}/diff/timeline",
    { params: { path: scope, query: to === undefined ? { from } : { from, to } }, signal },
  );
  return unwrap(data, error, response);
}

export function describeError(error: Error): string {
  return error instanceof ProblemError ? (error.problem.detail ?? error.message) : error.message;
}

function unwrap<T>(data: T | undefined, error: unknown, response: Response): T {
  if (data === undefined) {
    throw new ProblemError(toProblem(error, response));
  }
  return data;
}

function toProblem(error: unknown, response: Response): Problem {
  return isProblem(error) ? error : { title: response.statusText, status: response.status };
}

function isProblem(value: unknown): value is Problem {
  return typeof value === "object" && value !== null && ("title" in value || "status" in value);
}
