/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import type {
  EdgeMetrics,
  NodeType,
  TopologyEdge,
  TopologyGraph,
  TopologyNode,
} from "../api/client";
import { plural } from "../projects/format";

export const NODE_TYPES: readonly NodeType[] = ["SERVICE", "DATABASE", "TOPIC", "EXTERNAL"];

export interface NodeTypeLabel {
  readonly singular: string;
  readonly plural: string;
}

export const NODE_TYPE_LABELS: Record<NodeType, NodeTypeLabel> = {
  SERVICE: { singular: "Service", plural: "Services" },
  DATABASE: { singular: "Data store", plural: "Data stores" },
  TOPIC: { singular: "Data stream", plural: "Data streams" },
  EXTERNAL: { singular: "External", plural: "External" },
};

export type NodeTypeToken = "svc" | "db" | "topic" | "ext";

export const NODE_TYPE_TOKENS: Record<NodeType, NodeTypeToken> = {
  SERVICE: "svc",
  DATABASE: "db",
  TOPIC: "topic",
  EXTERNAL: "ext",
};

export type EdgeHealth = "ok" | "warn" | "bad";

const WARN_ERROR_RATE = 0.01;
const BAD_ERROR_RATE = 0.03;
const MAX_EDGE_WIDTH = 6;

export interface MapFilter {
  readonly hidden: readonly NodeType[];
}

export const EMPTY_MAP_FILTER: MapFilter = { hidden: [] };

export interface VisibleGraph {
  readonly nodes: readonly TopologyNode[];
  readonly edges: readonly TopologyEdge[];
}

export function parseMapFilter(params: URLSearchParams): MapFilter {
  const requested = params.getAll("hide");
  return { hidden: NODE_TYPES.filter((type) => requested.includes(type)) };
}

export function toMapParams(filter: MapFilter): URLSearchParams {
  const params = new URLSearchParams();
  filter.hidden.forEach((type) => {
    params.append("hide", type);
  });
  return params;
}

export function toggleNodeType(filter: MapFilter, type: NodeType): MapFilter {
  return {
    hidden: filter.hidden.includes(type)
      ? filter.hidden.filter((candidate) => candidate !== type)
      : NODE_TYPES.filter((candidate) => candidate === type || filter.hidden.includes(candidate)),
  };
}

export function countByType(nodes: readonly TopologyNode[]): Record<NodeType, number> {
  return nodes.reduce<Record<NodeType, number>>(
    (counts, node) => ({ ...counts, [node.type]: counts[node.type] + 1 }),
    { SERVICE: 0, DATABASE: 0, TOPIC: 0, EXTERNAL: 0 },
  );
}

export function visibleGraph(graph: TopologyGraph, filter: MapFilter): VisibleGraph {
  const nodes = graph.nodes.filter((node) => !filter.hidden.includes(node.type));
  const ids = new Set(nodes.map((node) => node.id));
  const edges = graph.edges.filter((edge) => ids.has(edge.sourceId) && ids.has(edge.targetId));
  return { nodes, edges };
}

export function errorRate(metrics: EdgeMetrics): number {
  return metrics.calls === 0 ? 0 : metrics.errors / metrics.calls;
}

export function edgeHealth(metrics: EdgeMetrics): EdgeHealth {
  const rate = errorRate(metrics);
  if (rate >= BAD_ERROR_RATE) {
    return "bad";
  }
  return rate >= WARN_ERROR_RATE ? "warn" : "ok";
}

export function edgeWidth(calls: number): number {
  return Math.min(MAX_EDGE_WIDTH, 1 + Math.log10(calls + 1));
}

export function edgeId(edge: TopologyEdge): string {
  return `${edge.sourceId}>${edge.targetId}:${edge.kind}`;
}

export function subtitle(node: TopologyNode): string {
  if (node.versions.length > 0) {
    return node.versions.map((version) => `v${version}`).join(", ");
  }
  const namespaces = [
    ...new Set(node.deployments.map((deployment) => deployment.namespace)),
  ].filter((namespace) => namespace !== "");
  return namespaces.length > 0 ? namespaces.join(", ") : node.id;
}

export function describeGraph(visible: VisibleGraph): string {
  const counts = countByType(visible.nodes);
  const streams = visible.edges.filter((edge) => edge.kind !== "SYNC").length;
  const sync = visible.edges.length - streams;
  return [
    plural(counts.SERVICE, "service"),
    plural(counts.DATABASE, "data store"),
    plural(counts.TOPIC, "data stream"),
    plural(counts.EXTERNAL, "external host"),
    `${plural(visible.edges.length, "dependency", "dependencies")} (${String(sync)} sync, ${String(streams)} stream)`,
  ].join(" · ");
}
