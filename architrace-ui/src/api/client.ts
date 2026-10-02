/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
import createClient from "openapi-fetch";
import type { components, paths } from "./schema";

export type Problem = components["schemas"]["Problem"];
export type ScopeSummary = components["schemas"]["ScopeSummary"];

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
