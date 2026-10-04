/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import {
  demoGraph,
  problem,
  requestOf,
  respondWithJson,
  respondWithText,
  scopeSummary,
} from "../test/http";
import { describeError, getGraph, listScopes, ProblemError } from "./client";

describe("listScopes", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("reads the scope summaries from the Query API", async () => {
    const scopes = [scopeSummary("webshop", "PROD", "k8s-prod-eu1")];
    const fetch = respondWithJson(scopes);
    vi.stubGlobal("fetch", fetch);

    await expect(listScopes()).resolves.toEqual(scopes);

    const request = requestOf(fetch);
    expect(request.method).toBe("GET");
    expect(new URL(request.url).pathname).toBe("/api/v1/scopes");
  });

  it("turns a problem response into a ProblemError", async () => {
    vi.stubGlobal("fetch", respondWithJson(problem, 404, "application/problem+json"));

    const error = await listScopes().catch((cause: unknown) => cause);

    expect(error).toBeInstanceOf(ProblemError);
    expect((error as ProblemError).problem).toEqual(problem);
    expect((error as ProblemError).message).toBe("Scope not found");
  });

  it("describes a response without problem details by its status", async () => {
    vi.stubGlobal("fetch", respondWithText("<html>bad gateway</html>", 502, "Bad Gateway"));

    const error = await listScopes().catch((cause: unknown) => cause);

    expect(error).toBeInstanceOf(ProblemError);
    expect((error as ProblemError).problem).toEqual({ title: "Bad Gateway", status: 502 });
  });

  it("accepts a problem that carries only a status", async () => {
    vi.stubGlobal("fetch", respondWithJson({ status: 503 }, 503, "application/problem+json"));

    const error = await listScopes().catch((cause: unknown) => cause);

    expect((error as ProblemError).problem).toEqual({ status: 503 });
  });

  it("names the status when the problem has no title", () => {
    const error = new ProblemError({ status: 503 });

    expect(error.message).toBe("The control plane answered 503");
  });
});

describe("getGraph", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("reads the graph of a scope with encoded path segments", async () => {
    const fetch = respondWithJson(demoGraph);
    vi.stubGlobal("fetch", fetch);

    await expect(
      getGraph({ project: "web shop", environment: "PROD", cluster: "k8s/prod" }, undefined),
    ).resolves.toEqual(demoGraph);

    const url = new URL(requestOf(fetch).url);
    expect(url.pathname).toBe("/api/v1/scopes/web%20shop/PROD/k8s%2Fprod/graph");
    expect(url.search).toBe("");
  });

  it("asks for the graph at a point in time", async () => {
    const fetch = respondWithJson(demoGraph);
    vi.stubGlobal("fetch", fetch);

    await getGraph(demoGraph.scope, "2026-10-01T12:00:00Z");

    expect(new URL(requestOf(fetch).url).search).toBe("?at=2026-10-01T12%3A00%3A00Z");
  });

  it("turns a problem response into a ProblemError", async () => {
    vi.stubGlobal("fetch", respondWithJson(problem, 404, "application/problem+json"));

    const error = await getGraph(demoGraph.scope, undefined).catch((cause: unknown) => cause);

    expect(error).toBeInstanceOf(ProblemError);
    expect((error as ProblemError).problem).toEqual(problem);
  });
});

describe("describeError", () => {
  it("prefers the problem detail, then the message", () => {
    expect(describeError(new ProblemError(problem))).toBe(problem.detail);
    expect(describeError(new ProblemError({ title: "Gone", status: 410 }))).toBe("Gone");
    expect(describeError(new Error("network down"))).toBe("network down");
  });
});
