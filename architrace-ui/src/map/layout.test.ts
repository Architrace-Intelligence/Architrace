/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { describe, expect, it } from "vitest";
import { fixtureGraph, LARGE_FIXTURE } from "../test/graph";
import { demoGraph } from "../test/http";
import { layoutGraph } from "./layout";

describe("layoutGraph", () => {
  it("layers dependencies from left to right", async () => {
    const positions = await layoutGraph(demoGraph.nodes, demoGraph.edges);
    const x = (id: string) => positions.get(id)?.x ?? Number.NaN;

    expect(positions.size).toBe(demoGraph.nodes.length);
    expect(x("service:api-gateway")).toBeLessThan(x("service:orders-service"));
    expect(x("service:orders-service")).toBeLessThan(x("db:postgresql/orders"));
    expect(x("service:orders-service")).toBeLessThan(x("topic:kafka/order-events"));
    expect(x("topic:kafka/order-events")).toBeLessThan(x("service:notification-service"));
  });

  it("places a 300-node, 900-edge graph on distinct positions", async () => {
    const graph = fixtureGraph();
    expect(graph.nodes).toHaveLength(300);
    expect(graph.edges).toHaveLength(LARGE_FIXTURE.edges);

    const positions = await layoutGraph(graph.nodes, graph.edges);

    expect(positions.size).toBe(300);
    const distinct = new Set(
      [...positions.values()].map((point) => `${String(point.x)},${String(point.y)}`),
    );
    expect(distinct.size).toBe(300);
  }, 60_000);
});
