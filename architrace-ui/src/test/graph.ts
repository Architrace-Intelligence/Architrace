/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import type { EdgeKind, TopologyEdge, TopologyGraph, TopologyNode } from "../api/client";

export interface FixtureShape {
  readonly services: number;
  readonly tiers: number;
  readonly databases: number;
  readonly topics: number;
  readonly externals: number;
  readonly edges: number;
}

export const LARGE_FIXTURE: FixtureShape = {
  services: 180,
  tiers: 6,
  databases: 60,
  topics: 40,
  externals: 20,
  edges: 900,
};

function random(seed: number): () => number {
  let state = seed;
  return () => {
    state = (state * 1103515245 + 12345) & 0x7fffffff;
    return state / 0x7fffffff;
  };
}

function service(index: number): TopologyNode {
  return {
    id: `service:s${String(index)}`,
    type: "SERVICE",
    name: `service-${String(index)}`,
    versions: [`1.${String(index % 9)}.0`],
    deployments: [{ cluster: "k8s-prod-eu1", namespace: `ns-${String(index % 12)}` }],
    labels: {},
  };
}

function leaf(type: TopologyNode["type"], id: string, name: string): TopologyNode {
  return { id, type, name, versions: [], deployments: [], labels: {} };
}

function edge(sourceId: string, targetId: string, kind: EdgeKind, calls: number): TopologyEdge {
  return {
    sourceId,
    targetId,
    kind,
    metrics: {
      calls,
      errors: Math.floor(calls / 500),
      p50Millis: 4,
      p95Millis: 20,
      p99Millis: 60,
      maxMillis: 300,
    },
  };
}

export function fixtureGraph(shape: FixtureShape = LARGE_FIXTURE): TopologyGraph {
  const next = random(42);
  const services = Array.from({ length: shape.services }, (_, index) => service(index));
  const tierOf = (index: number) => Math.floor((index * shape.tiers) / shape.services);
  const databases = Array.from({ length: shape.databases }, (_, index) =>
    leaf("DATABASE", `db:postgresql/d${String(index)}`, `postgresql/d${String(index)}`),
  );
  const topics = Array.from({ length: shape.topics }, (_, index) =>
    leaf("TOPIC", `topic:kafka/t${String(index)}`, `t${String(index)}-events`),
  );
  const externals = Array.from({ length: shape.externals }, (_, index) =>
    leaf("EXTERNAL", `external:host${String(index)}.example`, `host${String(index)}.example`),
  );
  const edges = new Map<string, TopologyEdge>();
  const add = (candidate: TopologyEdge) => {
    if (candidate.sourceId !== candidate.targetId && edges.size < shape.edges) {
      edges.set(`${candidate.sourceId}>${candidate.targetId}:${candidate.kind}`, candidate);
    }
  };
  const pick = <T>(items: readonly T[]): T | undefined => items[Math.floor(next() * items.length)];
  services.forEach((node, index) => {
    const database = databases[index % Math.max(1, databases.length)];
    if (database !== undefined) {
      add(edge(node.id, database.id, "SYNC", 24_000));
    }
    const downstream = services.filter((_, other) => tierOf(other) === tierOf(index) + 1);
    Array.from({ length: 3 }, () => pick(downstream)).forEach((callee) => {
      if (callee !== undefined) {
        add(edge(node.id, callee.id, "SYNC", 8_700));
      }
    });
    const topic = topics[index % Math.max(1, topics.length)];
    const consumed = topics[(index + 7) % Math.max(1, topics.length)];
    if (topic !== undefined && index % 3 === 0) {
      add(edge(node.id, topic.id, "PUBLISH", 2_000));
    }
    if (consumed !== undefined && index % 4 === 0) {
      add(edge(consumed.id, node.id, "CONSUME", 2_000));
    }
    const external = externals[index % Math.max(1, externals.length)];
    if (external !== undefined && index % 5 === 0) {
      add(edge(node.id, external.id, "SYNC", 1_200));
    }
  });
  let guard = 0;
  while (edges.size < shape.edges && guard < 100_000) {
    guard += 1;
    const caller = pick(services);
    const callee = pick(services);
    if (
      caller !== undefined &&
      callee !== undefined &&
      tierOf(services.indexOf(caller)) < tierOf(services.indexOf(callee))
    ) {
      add(edge(caller.id, callee.id, "SYNC", 500));
    }
  }
  return {
    scope: { project: "fixture", environment: "PROD", cluster: "k8s-prod-eu1" },
    at: "2026-10-01T12:00:00Z",
    nodes: [...services, ...databases, ...topics, ...externals],
    edges: [...edges.values()],
  };
}
