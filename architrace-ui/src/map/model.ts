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

export type Lens = "all" | "streams";

export type Selection =
  { readonly kind: "node"; readonly id: string } | { readonly kind: "edge"; readonly id: string };

export interface MapState {
  readonly hidden: readonly NodeType[];
  readonly namespace: string | undefined;
  readonly lens: Lens;
  readonly query: string;
  readonly at: string | undefined;
  readonly selection: Selection | undefined;
}

export const INITIAL_MAP_STATE: MapState = {
  hidden: [],
  namespace: undefined,
  lens: "all",
  query: "",
  at: undefined,
  selection: undefined,
};

export interface VisibleGraph {
  readonly nodes: readonly TopologyNode[];
  readonly edges: readonly TopologyEdge[];
}

export interface Dependency {
  readonly edge: TopologyEdge;
  readonly other: TopologyNode;
}

export interface NodeDetails {
  readonly node: TopologyNode;
  readonly inbound: readonly Dependency[];
  readonly outbound: readonly Dependency[];
}

export interface EdgeDetails {
  readonly edge: TopologyEdge;
  readonly source: TopologyNode;
  readonly target: TopologyNode;
}

export interface StreamSummary {
  readonly topic: TopologyNode;
  readonly producers: readonly TopologyNode[];
  readonly consumers: readonly TopologyNode[];
  readonly messages: number;
}

export interface NodeLook {
  readonly selected: boolean;
  readonly dimmed: boolean;
  readonly match: boolean;
}

export interface EdgeLook {
  readonly touching: boolean;
  readonly dimmed: boolean;
}

export interface Looks {
  readonly nodes: ReadonlyMap<string, NodeLook>;
  readonly edges: ReadonlyMap<string, EdgeLook>;
}

export function parseMapState(params: URLSearchParams): MapState {
  const requested = params.getAll("hide");
  const node = params.get("node");
  const edge = params.get("edge");
  const namespace = params.get("ns");
  return {
    hidden: NODE_TYPES.filter((type) => requested.includes(type)),
    namespace: namespace === null || namespace === "" ? undefined : namespace,
    lens: params.get("lens") === "streams" ? "streams" : "all",
    query: params.get("q") ?? "",
    at: parseAt(params.get("at")),
    selection: parseSelection(node, edge),
  };
}

function parseAt(value: string | null): string | undefined {
  return value === null || Number.isNaN(Date.parse(value)) ? undefined : value;
}

function parseSelection(node: string | null, edge: string | null): Selection | undefined {
  if (node !== null && node !== "") {
    return { kind: "node", id: node };
  }
  return edge !== null && edge !== "" ? { kind: "edge", id: edge } : undefined;
}

export function toMapParams(state: MapState): URLSearchParams {
  const params = new URLSearchParams();
  state.hidden.forEach((type) => {
    params.append("hide", type);
  });
  if (state.namespace !== undefined) {
    params.set("ns", state.namespace);
  }
  if (state.lens !== "all") {
    params.set("lens", state.lens);
  }
  if (state.query !== "") {
    params.set("q", state.query);
  }
  if (state.at !== undefined) {
    params.set("at", state.at);
  }
  if (state.selection !== undefined) {
    params.set(state.selection.kind, state.selection.id);
  }
  return params;
}

export function toggleNodeType(state: MapState, type: NodeType): MapState {
  return {
    ...state,
    hidden: state.hidden.includes(type)
      ? state.hidden.filter((candidate) => candidate !== type)
      : NODE_TYPES.filter((candidate) => candidate === type || state.hidden.includes(candidate)),
  };
}

export function countByType(nodes: readonly TopologyNode[]): Record<NodeType, number> {
  return nodes.reduce<Record<NodeType, number>>(
    (counts, node) => ({ ...counts, [node.type]: counts[node.type] + 1 }),
    { SERVICE: 0, DATABASE: 0, TOPIC: 0, EXTERNAL: 0 },
  );
}

export function visibleGraph(graph: TopologyGraph, state: MapState): VisibleGraph {
  const shown = graph.nodes.filter((node) => !state.hidden.includes(node.type));
  const nodes =
    state.namespace === undefined ? shown : neighbourhood(graph, shown, state.namespace);
  const ids = new Set(nodes.map((node) => node.id));
  const edges = graph.edges.filter((edge) => ids.has(edge.sourceId) && ids.has(edge.targetId));
  return { nodes, edges };
}

function neighbourhood(
  graph: TopologyGraph,
  shown: readonly TopologyNode[],
  namespace: string,
): TopologyNode[] {
  const inside = new Set(
    shown.filter((node) => namespacesOf(node).includes(namespace)).map((node) => node.id),
  );
  const adjacent = new Set(
    graph.edges
      .filter((edge) => inside.has(edge.sourceId) || inside.has(edge.targetId))
      .flatMap((edge) => [edge.sourceId, edge.targetId]),
  );
  return shown.filter((node) => inside.has(node.id) || adjacent.has(node.id));
}

export function graphNamespaces(graph: TopologyGraph): string[] {
  return [...new Set(graph.nodes.flatMap(namespacesOf))].sort((left, right) =>
    left.localeCompare(right),
  );
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
  const namespaces = namespacesOf(node);
  return namespaces.length > 0 ? namespaces.join(", ") : node.id;
}

export function namespacesOf(node: TopologyNode): string[] {
  return [...new Set(node.deployments.map((deployment) => deployment.namespace))].filter(
    (namespace) => namespace !== "",
  );
}

export function clustersOf(node: TopologyNode): string[] {
  return [...new Set(node.deployments.map((deployment) => deployment.cluster))];
}

export function matchesQuery(node: TopologyNode, query: string): boolean {
  const needle = query.trim().toLowerCase();
  return (
    needle === "" ||
    [node.name, node.id, subtitle(node)].some((value) => value.toLowerCase().includes(needle))
  );
}

export function nodeDetails(graph: VisibleGraph, id: string): NodeDetails | undefined {
  const byId = new Map(graph.nodes.map((node) => [node.id, node]));
  const node = byId.get(id);
  if (node === undefined) {
    return undefined;
  }
  const dependency = (edge: TopologyEdge, otherId: string): Dependency[] => {
    const other = byId.get(otherId);
    return other === undefined ? [] : [{ edge, other }];
  };
  return {
    node,
    inbound: graph.edges
      .filter((edge) => edge.targetId === id)
      .flatMap((edge) => dependency(edge, edge.sourceId)),
    outbound: graph.edges
      .filter((edge) => edge.sourceId === id)
      .flatMap((edge) => dependency(edge, edge.targetId)),
  };
}

export function edgeDetails(graph: VisibleGraph, id: string): EdgeDetails | undefined {
  const edge = graph.edges.find((candidate) => edgeId(candidate) === id);
  const source = graph.nodes.find((node) => node.id === edge?.sourceId);
  const target = graph.nodes.find((node) => node.id === edge?.targetId);
  return edge === undefined || source === undefined || target === undefined
    ? undefined
    : { edge, source, target };
}

export function streams(graph: VisibleGraph): StreamSummary[] {
  const byId = new Map(graph.nodes.map((node) => [node.id, node]));
  return graph.nodes
    .filter((node) => node.type === "TOPIC")
    .map((topic) => {
      const published = graph.edges.filter(
        (edge) => edge.kind === "PUBLISH" && edge.targetId === topic.id,
      );
      const consumed = graph.edges.filter(
        (edge) => edge.kind === "CONSUME" && edge.sourceId === topic.id,
      );
      return {
        topic,
        producers: published.flatMap((edge) => byId.get(edge.sourceId) ?? []),
        consumers: consumed.flatMap((edge) => byId.get(edge.targetId) ?? []),
        messages: published.reduce((total, edge) => total + edge.metrics.calls, 0),
      };
    })
    .sort((left, right) => left.topic.name.localeCompare(right.topic.name));
}

export function looks(visible: VisibleGraph, state: MapState): Looks {
  const selection = isPresent(visible, state.selection) ? state.selection : undefined;
  const lit = litNodes(visible, selection);
  const touching = touchingEdges(visible, selection);
  const streamNodes = new Set(
    visible.edges
      .filter((edge) => edge.kind !== "SYNC")
      .flatMap((edge) => [edge.sourceId, edge.targetId]),
  );
  const querying = state.query.trim() !== "";
  const nodes = new Map(
    visible.nodes.map((node) => {
      const match = querying && matchesQuery(node, state.query);
      const dimmed =
        (lit !== undefined && !lit.has(node.id)) ||
        (querying && !match) ||
        (state.lens === "streams" && !streamNodes.has(node.id));
      return [node.id, { selected: isSelectedNode(selection, node.id), dimmed, match }];
    }),
  );
  const edges = new Map(
    visible.edges.map((edge) => {
      const id = edgeId(edge);
      const isTouching = touching.has(id);
      const dimmed =
        (nodes.get(edge.sourceId)?.dimmed ?? false) ||
        (nodes.get(edge.targetId)?.dimmed ?? false) ||
        (selection !== undefined && !isTouching) ||
        (state.lens === "streams" && edge.kind === "SYNC");
      return [id, { touching: isTouching, dimmed }];
    }),
  );
  return { nodes, edges };
}

function isPresent(visible: VisibleGraph, selection: Selection | undefined): boolean {
  if (selection === undefined) {
    return false;
  }
  return selection.kind === "node"
    ? visible.nodes.some((node) => node.id === selection.id)
    : visible.edges.some((edge) => edgeId(edge) === selection.id);
}

function isSelectedNode(selection: Selection | undefined, id: string): boolean {
  return selection?.kind === "node" && selection.id === id;
}

function litNodes(
  visible: VisibleGraph,
  selection: Selection | undefined,
): ReadonlySet<string> | undefined {
  if (selection === undefined) {
    return undefined;
  }
  if (selection.kind === "edge") {
    const edge = visible.edges.find((candidate) => edgeId(candidate) === selection.id);
    return new Set(edge === undefined ? [] : [edge.sourceId, edge.targetId]);
  }
  return new Set([
    selection.id,
    ...visible.edges
      .filter((edge) => edge.sourceId === selection.id || edge.targetId === selection.id)
      .flatMap((edge) => [edge.sourceId, edge.targetId]),
  ]);
}

function touchingEdges(
  visible: VisibleGraph,
  selection: Selection | undefined,
): ReadonlySet<string> {
  if (selection === undefined) {
    return new Set();
  }
  if (selection.kind === "edge") {
    return new Set([selection.id]);
  }
  return new Set(
    visible.edges
      .filter((edge) => edge.sourceId === selection.id || edge.targetId === selection.id)
      .map(edgeId),
  );
}

export function describeGraph(visible: VisibleGraph): string {
  const counts = countByType(visible.nodes);
  const streamEdges = visible.edges.filter((edge) => edge.kind !== "SYNC").length;
  const sync = visible.edges.length - streamEdges;
  return [
    plural(counts.SERVICE, "service"),
    plural(counts.DATABASE, "data store"),
    plural(counts.TOPIC, "data stream"),
    plural(counts.EXTERNAL, "external host"),
    `${plural(visible.edges.length, "dependency", "dependencies")} (${String(sync)} sync, ${String(streamEdges)} stream)`,
  ].join(" · ");
}

export function describeMatches(visible: VisibleGraph, query: string): string {
  const matches = visible.nodes.filter((node) => matchesQuery(node, query)).length;
  return `${String(matches)} of ${plural(visible.nodes.length, "node")} match “${query.trim()}”`;
}
