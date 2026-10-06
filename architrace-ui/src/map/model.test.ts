/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { describe, expect, it } from "vitest";
import type { EdgeMetrics, TopologyNode } from "../api/client";
import { demoGraph, platformGraph } from "../test/http";
import {
  countByType,
  describeGraph,
  describeMatches,
  edgeDetails,
  edgeHealth,
  edgeId,
  edgeWidth,
  errorRate,
  graphNamespaces,
  INITIAL_MAP_STATE,
  looks,
  matchesQuery,
  nodeDetails,
  parseMapState,
  streams,
  subtitle,
  toggleNodeType,
  toMapParams,
  visibleGraph,
  isPlatformHost,
  PLATFORM_GROUP_ID,
  platformHosts,
  platformView,
} from "./model";

function metrics(calls: number, errors: number): EdgeMetrics {
  return { calls, errors, p50Millis: 1, p95Millis: 2, p99Millis: 3, maxMillis: 4 };
}

function node(overrides: Partial<TopologyNode>): TopologyNode {
  return {
    id: "service:orders",
    type: "SERVICE",
    name: "orders",
    versions: [],
    deployments: [],
    labels: {},
    ...overrides,
  };
}

describe("map state", () => {
  it("reads every field from the URL and writes back only what differs from the defaults", () => {
    const state = parseMapState(
      new URLSearchParams(
        "hide=TOPIC&hide=SERVICE&hide=BOGUS&platform=on&lens=streams&q=ord&at=2026-10-01T12:00:00Z&node=service:orders-service",
      ),
    );

    expect(state).toEqual({
      hidden: ["SERVICE", "TOPIC"],
      platform: true,
      namespace: undefined,
      lens: "streams",
      query: "ord",
      at: "2026-10-01T12:00:00Z",
      selection: { kind: "node", id: "service:orders-service" },
    });
    expect(toMapParams(state).toString()).toBe(
      "hide=SERVICE&hide=TOPIC&platform=on&lens=streams&q=ord&at=2026-10-01T12%3A00%3A00Z&node=service%3Aorders-service",
    );
    expect(parseMapState(new URLSearchParams())).toEqual(INITIAL_MAP_STATE);
    expect(toMapParams(INITIAL_MAP_STATE).toString()).toBe("");
  });

  it("ignores an invalid time, an unknown lens, an empty namespace and an empty selection", () => {
    expect(
      parseMapState(new URLSearchParams("at=yesterday&lens=sideways&ns=&node=&edge=")),
    ).toEqual(INITIAL_MAP_STATE);
    const namespaced = parseMapState(new URLSearchParams("ns=orders"));
    expect(namespaced.namespace).toBe("orders");
    expect(toMapParams(namespaced).toString()).toBe("ns=orders");
  });

  it("prefers the node over the edge and keeps an edge selection", () => {
    expect(parseMapState(new URLSearchParams("node=a&edge=b")).selection).toEqual({
      kind: "node",
      id: "a",
    });
    const edge = parseMapState(new URLSearchParams("edge=a>b:SYNC"));
    expect(edge.selection).toEqual({ kind: "edge", id: "a>b:SYNC" });
    expect(toMapParams(edge).toString()).toBe("edge=a%3Eb%3ASYNC");
  });

  it("toggles a node type in and out of the hidden set", () => {
    const hidden = toggleNodeType(INITIAL_MAP_STATE, "DATABASE");

    expect(hidden.hidden).toEqual(["DATABASE"]);
    expect(toggleNodeType(hidden, "SERVICE").hidden).toEqual(["SERVICE", "DATABASE"]);
    expect(toggleNodeType(hidden, "DATABASE").hidden).toEqual([]);
  });
});

describe("visibleGraph", () => {
  it("counts nodes by type", () => {
    expect(countByType(demoGraph.nodes)).toEqual({
      SERVICE: 5,
      DATABASE: 2,
      TOPIC: 2,
      EXTERNAL: 1,
    });
  });

  it("drops hidden nodes and every edge that touches them", () => {
    const visible = visibleGraph(demoGraph, {
      ...INITIAL_MAP_STATE,
      hidden: ["DATABASE", "TOPIC"],
    });

    expect(visible.nodes).toHaveLength(6);
    expect(visible.nodes.map((candidate) => candidate.type)).not.toContain("DATABASE");
    expect(visible.edges).toHaveLength(5);
    expect(visible.edges.every((edge) => edge.kind === "SYNC")).toBe(true);
  });

  it("keeps the services of a namespace and everything they touch", () => {
    const visible = visibleGraph(demoGraph, { ...INITIAL_MAP_STATE, namespace: "orders" });

    expect(visible.nodes.map((candidate) => candidate.name).sort()).toEqual([
      "api-gateway",
      "inventory-service",
      "order-events",
      "orders-service",
      "postgresql/orders",
    ]);
    expect(visible.edges).toHaveLength(5);
    expect(graphNamespaces(demoGraph)).toEqual([
      "edge",
      "inventory",
      "notify",
      "orders",
      "payments",
    ]);
    expect(visibleGraph(demoGraph, { ...INITIAL_MAP_STATE, namespace: "nowhere" }).nodes).toEqual(
      [],
    );
  });

  it("describes the visible graph and the matches of a query in one line", () => {
    const visible = visibleGraph(demoGraph, INITIAL_MAP_STATE);

    expect(describeGraph(visible)).toBe(
      "5 services · 2 data stores · 2 data streams · 1 external host · 11 dependencies (7 sync, 4 stream)",
    );
    expect(describeGraph({ nodes: [node({})], edges: [] })).toBe(
      "1 service · 0 data stores · 0 data streams · 0 external hosts · 0 dependencies (0 sync, 0 stream)",
    );
    expect(describeMatches(visible, " order ")).toBe("3 of 10 nodes match “order”");
  });
});

describe("platform hosts", () => {
  const shown = { ...INITIAL_MAP_STATE, platform: true };

  it("keeps the platform hosts off the map by default and leaves the rest untouched", () => {
    expect(platformHosts(platformGraph).map((host) => host.id)).toEqual([
      "external:flags.internal",
      "external:config.internal",
    ]);
    expect(platformGraph.nodes.filter(isPlatformHost)).toHaveLength(2);

    const visible = visibleGraph(platformGraph, INITIAL_MAP_STATE);

    expect(visible.nodes).toEqual(demoGraph.nodes);
    expect(visible.edges).toEqual(demoGraph.edges);
    expect(platformView(demoGraph, shown)).toBe(demoGraph);
  });

  it("groups the platform hosts into one node and merges the calls of every caller", () => {
    const slower = {
      ...platformGraph,
      edges: platformGraph.edges.map((edge) =>
        edge.targetId === "external:config.internal"
          ? { ...edge, metrics: { ...edge.metrics, p95Millis: 40 } }
          : edge,
      ),
    };

    const visible = visibleGraph(slower, shown);
    const group = visible.nodes.find((node) => node.id === PLATFORM_GROUP_ID);

    expect(visible.nodes).toHaveLength(demoGraph.nodes.length + 1);
    expect(group).toEqual({
      id: "platform",
      type: "EXTERNAL",
      name: "Platform",
      versions: [],
      deployments: [],
      labels: { category: "platform", hosts: "config.internal, flags.internal" },
    });
    expect(group === undefined ? "" : subtitle(group)).toBe("config.internal, flags.internal");
    expect(group !== undefined && matchesQuery(group, "flags")).toBe(true);
    expect(visible.edges).toHaveLength(demoGraph.edges.length + 2);
    expect(
      visible.edges.find((edge) => edgeId(edge) === "service:orders-service>platform:SYNC")
        ?.metrics,
    ).toEqual({
      calls: 150,
      errors: 1,
      p50Millis: 4,
      p95Millis: 40,
      p99Millis: 60,
      maxMillis: 300,
    });
    expect(
      visible.edges.find((edge) => edgeId(edge) === "service:payments-service>platform:SYNC")
        ?.metrics.calls,
    ).toBe(30);
    expect(
      visibleGraph(platformGraph, { ...shown, hidden: ["EXTERNAL"] }).nodes,
    ).not.toContainEqual(group);
  });

  it("dims the calls to the platform group until one of their ends is selected", () => {
    const visible = visibleGraph(platformGraph, shown);
    const orders = "service:orders-service>platform:SYNC";
    const payments = "service:payments-service>platform:SYNC";

    expect(looks(visible, shown).edges.get(orders)).toEqual({ touching: false, dimmed: true });
    expect(looks(visible, shown).nodes.get(PLATFORM_GROUP_ID)?.dimmed).toBe(false);

    const caller = looks(visible, {
      ...shown,
      selection: { kind: "node", id: "service:orders-service" },
    });
    expect(caller.edges.get(orders)).toEqual({ touching: true, dimmed: false });
    expect(caller.edges.get(payments)?.dimmed).toBe(true);

    const group = looks(visible, { ...shown, selection: { kind: "node", id: PLATFORM_GROUP_ID } });
    expect(group.edges.get(orders)?.dimmed).toBe(false);
    expect(group.edges.get(payments)?.dimmed).toBe(false);
  });
});

describe("details", () => {
  it("lists the inbound and outbound dependencies of a node with the other end embedded", () => {
    const details = nodeDetails(demoGraph, "service:orders-service");
    const describe = (dependencies: readonly { other: TopologyNode; edge: { kind: string } }[]) =>
      dependencies.map((dependency) => `${dependency.other.name}:${dependency.edge.kind}`);

    expect(details?.node.name).toBe("orders-service");
    expect(describe(details?.inbound ?? [])).toEqual([
      "api-gateway:SYNC",
      "inventory-service:SYNC",
    ]);
    expect(describe(details?.outbound ?? [])).toEqual([
      "postgresql/orders:SYNC",
      "inventory-service:SYNC",
      "order-events:PUBLISH",
    ]);
    expect(nodeDetails(demoGraph, "service:ghost")).toBeUndefined();
  });

  it("resolves an edge with both ends and rejects unknown ids", () => {
    const details = edgeDetails(demoGraph, "service:orders-service>db:postgresql/orders:SYNC");

    expect(details?.source.name).toBe("orders-service");
    expect(details?.target.name).toBe("postgresql/orders");
    expect(details?.edge.metrics.calls).toBe(24_000);
    expect(edgeDetails(demoGraph, "a>b:SYNC")).toBeUndefined();
  });

  it("summarises data streams with producers, consumers and messages", () => {
    const summaries = streams(demoGraph);

    expect(summaries.map((stream) => stream.topic.name)).toEqual([
      "order-events",
      "payment-events",
    ]);
    expect(summaries[0]?.producers.map((producer) => producer.name)).toEqual(["orders-service"]);
    expect(summaries[0]?.consumers.map((consumer) => consumer.name)).toEqual([
      "notification-service",
    ]);
    expect(summaries[0]?.messages).toBe(8_700);
  });
});

describe("looks", () => {
  const visible = visibleGraph(demoGraph, INITIAL_MAP_STATE);

  it("lights the selected node, its neighbours and the touching edges and dims the rest", () => {
    const result = looks(visible, {
      ...INITIAL_MAP_STATE,
      selection: { kind: "node", id: "service:orders-service" },
    });

    expect(result.nodes.get("service:orders-service")).toEqual({
      selected: true,
      dimmed: false,
      match: false,
    });
    expect(result.nodes.get("service:api-gateway")?.dimmed).toBe(false);
    expect(result.nodes.get("service:payments-service")?.dimmed).toBe(true);
    expect(result.edges.get("service:api-gateway>service:orders-service:SYNC")).toEqual({
      touching: true,
      dimmed: false,
    });
    expect(result.edges.get("service:api-gateway>service:payments-service:SYNC")).toEqual({
      touching: false,
      dimmed: true,
    });
  });

  it("lights only the two ends of a selected edge", () => {
    const id = "service:payments-service>external:api.stripe.com:SYNC";
    const result = looks(visible, { ...INITIAL_MAP_STATE, selection: { kind: "edge", id } });

    expect(result.nodes.get("service:payments-service")?.dimmed).toBe(false);
    expect(result.nodes.get("external:api.stripe.com")?.dimmed).toBe(false);
    expect(result.nodes.get("service:api-gateway")?.dimmed).toBe(true);
    expect(result.edges.get(id)?.touching).toBe(true);
    expect([...result.edges.values()].filter((edge) => edge.touching)).toHaveLength(1);
  });

  it("ignores a selection that is not in the visible graph", () => {
    const result = looks(visible, {
      ...INITIAL_MAP_STATE,
      selection: { kind: "node", id: "service:ghost" },
    });

    expect([...result.nodes.values()].every((look) => !look.dimmed && !look.selected)).toBe(true);
    expect([...result.edges.values()].every((look) => !look.dimmed && !look.touching)).toBe(true);
  });

  it("marks the matches of a query and dims everything else", () => {
    const result = looks(visible, { ...INITIAL_MAP_STATE, query: "ORDER" });

    expect(result.nodes.get("service:orders-service")).toEqual({
      selected: false,
      dimmed: false,
      match: true,
    });
    expect(result.nodes.get("db:postgresql/orders")?.match).toBe(true);
    expect(result.nodes.get("topic:kafka/order-events")?.match).toBe(true);
    expect(result.nodes.get("service:api-gateway")?.dimmed).toBe(true);
    expect(result.edges.get("service:orders-service>db:postgresql/orders:SYNC")?.dimmed).toBe(
      false,
    );
    expect(result.edges.get("service:api-gateway>service:orders-service:SYNC")?.dimmed).toBe(true);
  });

  it("recedes synchronous calls and nodes without a stream under the Data streams lens", () => {
    const result = looks(visible, { ...INITIAL_MAP_STATE, lens: "streams" });

    expect(result.nodes.get("topic:kafka/order-events")?.dimmed).toBe(false);
    expect(result.nodes.get("service:notification-service")?.dimmed).toBe(false);
    expect(result.nodes.get("db:postgresql/orders")?.dimmed).toBe(true);
    expect(
      result.edges.get("service:orders-service>topic:kafka/order-events:PUBLISH")?.dimmed,
    ).toBe(false);
    expect(result.edges.get("service:orders-service>db:postgresql/orders:SYNC")?.dimmed).toBe(true);
  });
});

describe("edge encoding", () => {
  it("classifies the health by error rate with thresholds at 1 % and 3 %", () => {
    expect(errorRate(metrics(0, 0))).toBe(0);
    expect(edgeHealth(metrics(0, 0))).toBe("ok");
    expect(edgeHealth(metrics(1000, 9))).toBe("ok");
    expect(edgeHealth(metrics(1000, 10))).toBe("warn");
    expect(edgeHealth(metrics(1000, 29))).toBe("warn");
    expect(edgeHealth(metrics(1000, 30))).toBe("bad");
  });

  it("grows the width with the calls on a log scale and caps it", () => {
    expect(edgeWidth(0)).toBe(1);
    expect(edgeWidth(9)).toBeCloseTo(2);
    expect(edgeWidth(999)).toBeCloseTo(4);
    expect(edgeWidth(10_000_000)).toBe(6);
    expect(edgeWidth(500)).toBeGreaterThan(edgeWidth(50));
  });

  it("names an edge by its ends and kind", () => {
    const publish = demoGraph.edges.find((edge) => edge.kind === "PUBLISH");

    expect(publish === undefined ? "" : edgeId(publish)).toBe(
      "service:orders-service>topic:kafka/order-events:PUBLISH",
    );
  });
});

describe("subtitle", () => {
  it("prefers the versions, then the namespaces, then the id", () => {
    expect(subtitle(node({ versions: ["4.1.2", "4.1.3"] }))).toBe("v4.1.2, v4.1.3");
    expect(
      subtitle(
        node({
          deployments: [
            { cluster: "a", namespace: "orders" },
            { cluster: "b", namespace: "orders" },
            { cluster: "c", namespace: "" },
            { cluster: "d", namespace: "billing" },
          ],
        }),
      ),
    ).toBe("orders, billing");
    expect(subtitle(node({ deployments: [{ cluster: "a", namespace: "" }] }))).toBe(
      "service:orders",
    );
    expect(subtitle(node({ id: "db:postgresql/orders", type: "DATABASE" }))).toBe(
      "db:postgresql/orders",
    );
  });

  it("matches a query against the name, the id and the subtitle", () => {
    const notify = node({
      name: "notification-service",
      deployments: [{ cluster: "a", namespace: "notify" }],
    });

    expect(matchesQuery(notify, "")).toBe(true);
    expect(matchesQuery(notify, "NOTIF")).toBe(true);
    expect(matchesQuery(notify, "service:")).toBe(true);
    expect(matchesQuery(notify, "notify")).toBe(true);
    expect(matchesQuery(notify, "orders-db")).toBe(false);
  });
});
