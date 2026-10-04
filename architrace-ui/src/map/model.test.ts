/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { describe, expect, it } from "vitest";
import type { EdgeMetrics, TopologyNode } from "../api/client";
import { demoGraph } from "../test/http";
import {
  countByType,
  describeGraph,
  edgeHealth,
  edgeId,
  edgeWidth,
  EMPTY_MAP_FILTER,
  errorRate,
  parseMapFilter,
  subtitle,
  toggleNodeType,
  toMapParams,
  visibleGraph,
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

describe("map filter", () => {
  it("reads the hidden node types from the URL and writes them back in a stable order", () => {
    const filter = parseMapFilter(new URLSearchParams("hide=TOPIC&hide=SERVICE&hide=BOGUS"));

    expect(filter.hidden).toEqual(["SERVICE", "TOPIC"]);
    expect(toMapParams(filter).toString()).toBe("hide=SERVICE&hide=TOPIC");
    expect(toMapParams(EMPTY_MAP_FILTER).toString()).toBe("");
  });

  it("toggles a node type in and out of the hidden set", () => {
    const hidden = toggleNodeType(EMPTY_MAP_FILTER, "DATABASE");

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
    const visible = visibleGraph(demoGraph, { hidden: ["DATABASE", "TOPIC"] });

    expect(visible.nodes).toHaveLength(6);
    expect(visible.nodes.map((candidate) => candidate.type)).not.toContain("DATABASE");
    expect(visible.edges).toHaveLength(5);
    expect(visible.edges.every((edge) => edge.kind === "SYNC")).toBe(true);
  });

  it("describes the visible graph in one line", () => {
    expect(describeGraph(visibleGraph(demoGraph, EMPTY_MAP_FILTER))).toBe(
      "5 services · 2 data stores · 2 data streams · 1 external host · 11 dependencies (7 sync, 4 stream)",
    );
    expect(describeGraph({ nodes: [node({})], edges: [] })).toBe(
      "1 service · 0 data stores · 0 data streams · 0 external hosts · 0 dependencies (0 sync, 0 stream)",
    );
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
});
