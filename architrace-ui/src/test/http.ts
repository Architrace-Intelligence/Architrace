/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { type Mock, vi } from "vitest";
import type {
  ScopeSummary,
  TopologyDiff,
  TopologyEdge,
  TopologyGraph,
  TopologyNode,
} from "../api/client";

export function respondWithJson(body: unknown, status = 200, contentType = "application/json") {
  return vi.fn<typeof fetch>(() =>
    Promise.resolve(
      new Response(JSON.stringify(body), { status, headers: { "content-type": contentType } }),
    ),
  );
}

export function respondByPath(routes: Readonly<Record<string, unknown>>) {
  return vi.fn<typeof fetch>((input) => {
    const url = new URL(input instanceof Request ? input.url : String(input));
    const body = Object.entries(routes).find(([suffix]) => url.pathname.endsWith(suffix))?.[1];
    return Promise.resolve(
      body === undefined
        ? new Response(JSON.stringify({ title: "Not Found", status: 404 }), {
            status: 404,
            headers: { "content-type": "application/problem+json" },
          })
        : new Response(JSON.stringify(body), {
            status: 200,
            headers: { "content-type": "application/json" },
          }),
    );
  });
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
    findings: { high: 0, medium: 0, low: 0 },
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

function node(
  type: TopologyNode["type"],
  id: string,
  name: string,
  overrides: Partial<Omit<TopologyNode, "id" | "type" | "name">> = {},
): TopologyNode {
  return { id, type, name, versions: [], deployments: [], labels: {}, ...overrides };
}

function edge(
  sourceId: string,
  targetId: string,
  kind: TopologyEdge["kind"],
  calls: number,
  errors: number,
): TopologyEdge {
  return {
    sourceId,
    targetId,
    kind,
    metrics: { calls, errors, p50Millis: 4, p95Millis: 20, p99Millis: 60, maxMillis: 300 },
  };
}

export const demoGraph: TopologyGraph = {
  scope: { project: "webshop", environment: "PROD", cluster: "k8s-prod-eu1" },
  at: "2026-10-01T12:00:00Z",
  nodes: [
    node("DATABASE", "db:postgresql/orders", "postgresql/orders"),
    node("DATABASE", "db:postgresql/payments", "postgresql/payments"),
    node("EXTERNAL", "external:api.stripe.com", "api.stripe.com"),
    node("SERVICE", "service:api-gateway", "api-gateway", {
      versions: ["3.4.0"],
      deployments: [{ cluster: "k8s-prod-eu1", namespace: "edge" }],
    }),
    node("SERVICE", "service:inventory-service", "inventory-service", {
      versions: ["1.9.0"],
      deployments: [{ cluster: "k8s-prod-eu1", namespace: "inventory" }],
    }),
    node("SERVICE", "service:notification-service", "notification-service", {
      deployments: [{ cluster: "k8s-prod-eu1", namespace: "notify" }],
    }),
    node("SERVICE", "service:orders-service", "orders-service", {
      versions: ["2.8.1"],
      deployments: [{ cluster: "k8s-prod-eu1", namespace: "orders" }],
      labels: { team: "orders" },
    }),
    node("SERVICE", "service:payments-service", "payments-service", {
      versions: ["4.1.2", "4.1.3"],
      deployments: [{ cluster: "k8s-prod-eu1", namespace: "payments" }],
    }),
    node("TOPIC", "topic:kafka/order-events", "order-events"),
    node("TOPIC", "topic:kafka/payment-events", "payment-events"),
  ],
  edges: [
    edge("service:api-gateway", "service:orders-service", "SYNC", 12_400, 37),
    edge("service:api-gateway", "service:payments-service", "SYNC", 3_100, 10),
    edge("service:inventory-service", "service:orders-service", "SYNC", 1_200, 0),
    edge("service:orders-service", "db:postgresql/orders", "SYNC", 24_000, 3),
    edge("service:orders-service", "service:inventory-service", "SYNC", 8_900, 12),
    edge("service:orders-service", "topic:kafka/order-events", "PUBLISH", 8_700, 0),
    edge("service:payments-service", "db:postgresql/payments", "SYNC", 5_000, 80),
    edge("service:payments-service", "external:api.stripe.com", "SYNC", 3_000, 150),
    edge("service:payments-service", "topic:kafka/payment-events", "PUBLISH", 2_000, 0),
    edge("topic:kafka/order-events", "service:notification-service", "CONSUME", 8_700, 0),
    edge("topic:kafka/payment-events", "service:notification-service", "CONSUME", 2_000, 0),
  ],
};

export const emptyGraph: TopologyGraph = { ...demoGraph, nodes: [], edges: [] };

export const demoDiff: TopologyDiff = {
  left: {
    scope: { project: "webshop", environment: "DEV", cluster: "k8s-dev-ci" },
    at: "2026-10-01T12:00:00Z",
  },
  right: {
    scope: { project: "webshop", environment: "PROD", cluster: "k8s-prod-eu1" },
    at: "2026-10-01T12:00:00Z",
  },
  nodesAdded: [
    node("SERVICE", "service:notification-service", "notification-service", {
      deployments: [{ cluster: "k8s-prod-eu1", namespace: "notify" }],
    }),
    node("TOPIC", "topic:kafka/payment-events", "payment-events"),
  ],
  nodesRemoved: [
    node("DATABASE", "db:postgresql/catalog", "postgresql/catalog"),
    node("SERVICE", "service:search-service", "search-service", {
      versions: ["2.3.0"],
      deployments: [{ cluster: "k8s-dev-ci", namespace: "search" }],
    }),
  ],
  nodesChanged: [
    {
      id: "service:orders-service",
      type: "SERVICE",
      name: "orders-service",
      versionsBefore: ["2.9.0"],
      versionsAfter: ["2.8.1"],
      deploymentsBefore: [{ cluster: "k8s-dev-ci", namespace: "orders" }],
      deploymentsAfter: [{ cluster: "k8s-prod-eu1", namespace: "orders" }],
    },
  ],
  edgesAdded: [
    {
      sourceId: "service:payments-service",
      targetId: "topic:kafka/payment-events",
      kind: "PUBLISH",
    },
    {
      sourceId: "topic:kafka/payment-events",
      targetId: "service:notification-service",
      kind: "CONSUME",
    },
  ],
  edgesRemoved: [
    { sourceId: "service:search-service", targetId: "db:postgresql/catalog", kind: "SYNC" },
    { sourceId: "service:search-service", targetId: "db:postgresql/orders", kind: "SYNC" },
  ],
};

export const emptyDiff: TopologyDiff = {
  ...demoDiff,
  nodesAdded: [],
  nodesRemoved: [],
  nodesChanged: [],
  edgesAdded: [],
  edgesRemoved: [],
};
