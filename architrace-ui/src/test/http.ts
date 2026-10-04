/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { type Mock, vi } from "vitest";
import type { ScopeSummary } from "../api/client";

export function respondWithJson(body: unknown, status = 200, contentType = "application/json") {
  return vi.fn<typeof fetch>(() =>
    Promise.resolve(
      new Response(JSON.stringify(body), { status, headers: { "content-type": contentType } }),
    ),
  );
}

export function respondWithText(body: string, status: number, statusText: string) {
  return vi.fn<typeof fetch>(() =>
    Promise.resolve(
      new Response(body, { status, statusText, headers: { "content-type": "text/html" } }),
    ),
  );
}

export function requestOf(mock: Mock<typeof fetch>): Request {
  const input = mock.mock.calls[0]?.[0];
  if (input instanceof Request) {
    return input;
  }
  throw new Error("fetch was not called with a Request");
}

export const problem = {
  type: "urn:architrace:problem:scope-not-found",
  title: "Scope not found",
  status: 404,
  detail: "no agent has registered for scope webshop/PROD/k8s-prod-eu2",
  instance: "/api/v1/scopes/webshop/PROD/k8s-prod-eu2/graph",
};

export function scopeSummary(
  project: string,
  environment: string,
  cluster: string,
  overrides: Partial<Omit<ScopeSummary, "scope">> = {},
): ScopeSummary {
  return {
    scope: { project, environment, cluster },
    agents: 2,
    liveAgents: 2,
    services: 14,
    dataStreams: 3,
    namespaces: 4,
    lastSnapshotAt: "2026-10-01T12:00:00Z",
    ...overrides,
  };
}

export const demoScopes: ScopeSummary[] = [
  scopeSummary("webshop", "PROD", "k8s-prod-eu1", { services: 8, dataStreams: 2, namespaces: 6 }),
  scopeSummary("webshop", "PROD", "k8s-prod-eu2", { agents: 1, liveAgents: 1, services: 3 }),
  scopeSummary("webshop", "DEV", "k8s-dev-eu1", { agents: 1, liveAgents: 1, services: 7 }),
  scopeSummary("webshop", "DEV", "k8s-dev-ci", {
    agents: 1,
    liveAgents: 0,
    services: 0,
    dataStreams: 0,
    namespaces: 0,
    lastSnapshotAt: "2026-10-01T10:00:00Z",
  }),
  scopeSummary("billing", "PROD", "k8s-prod-eu1", { agents: 1, liveAgents: 1, services: 4 }),
  scopeSummary("billing", "STAGE", "k8s-stage-eu1", {
    agents: 0,
    liveAgents: 0,
    services: 0,
    dataStreams: 0,
  }),
];
