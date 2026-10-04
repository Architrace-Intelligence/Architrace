/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import type {
  EdgeRef,
  NodeChange,
  NodeType,
  Scope,
  TopologyDiff,
  TopologyEdge,
  TopologyGraph,
  TopologyNode,
} from "../api/client";
import { formatInstant } from "../map/metrics";
import {
  type ChangeKind,
  type DriftOverlay,
  type EdgeChangeKind,
  edgeId,
  NODE_TYPE_LABELS,
  NODE_TYPES,
  type SideLabels,
  type VisibleGraph,
} from "../map/model";
import { plural } from "../projects/format";

export type DriftMode = "environments" | "timeline";
export type DriftView = "list" | "map";

export interface LeftSide {
  readonly environment: string;
  readonly cluster: string;
}

export interface DriftState {
  readonly mode: DriftMode;
  readonly left: LeftSide | undefined;
  readonly at: string | undefined;
  readonly from: string | undefined;
  readonly to: string | undefined;
  readonly view: DriftView;
  readonly node: string | undefined;
}

export const INITIAL_DRIFT_STATE: DriftState = {
  mode: "environments",
  left: undefined,
  at: undefined,
  from: undefined,
  to: undefined,
  view: "list",
  node: undefined,
};

const DAY_MILLIS = 24 * 60 * 60 * 1_000;

export function parseDriftState(params: URLSearchParams): DriftState {
  const leftEnvironment = params.get("left");
  const leftCluster = params.get("leftCluster");
  const node = params.get("node");
  return {
    mode: params.get("mode") === "timeline" ? "timeline" : "environments",
    left:
      leftEnvironment === null ||
      leftEnvironment === "" ||
      leftCluster === null ||
      leftCluster === ""
        ? undefined
        : { environment: leftEnvironment, cluster: leftCluster },
    at: parseInstant(params.get("at")),
    from: parseInstant(params.get("from")),
    to: parseInstant(params.get("to")),
    view: params.get("view") === "map" ? "map" : "list",
    node: node === null || node === "" ? undefined : node,
  };
}

function parseInstant(value: string | null): string | undefined {
  return value === null || Number.isNaN(Date.parse(value)) ? undefined : value;
}

export function toDriftParams(state: DriftState): URLSearchParams {
  const params = new URLSearchParams();
  if (state.mode !== "environments") {
    params.set("mode", state.mode);
  }
  if (state.mode === "environments") {
    if (state.left !== undefined) {
      params.set("left", state.left.environment);
      params.set("leftCluster", state.left.cluster);
    }
    if (state.at !== undefined) {
      params.set("at", state.at);
    }
  } else {
    if (state.from !== undefined) {
      params.set("from", state.from);
    }
    if (state.to !== undefined) {
      params.set("to", state.to);
    }
  }
  if (state.view !== "list") {
    params.set("view", state.view);
  }
  if (state.node !== undefined) {
    params.set("node", state.node);
  }
  return params;
}

export function isReady(state: DriftState): boolean {
  return state.mode === "environments" ? state.left !== undefined : state.from !== undefined;
}

export function otherScopes(scopes: readonly Scope[], right: Scope): Scope[] {
  return scopes
    .filter((scope) => scope.project === right.project && !sameScope(scope, right))
    .sort(
      (a, b) => a.environment.localeCompare(b.environment) || a.cluster.localeCompare(b.cluster),
    );
}

export function defaultLeft(others: readonly Scope[], right: Scope): LeftSide | undefined {
  const preferred = others.find((scope) => scope.environment !== right.environment) ?? others[0];
  return preferred === undefined
    ? undefined
    : { environment: preferred.environment, cluster: preferred.cluster };
}

export function defaultFrom(now: Date): string {
  return new Date(Math.floor((now.getTime() - DAY_MILLIS) / 1_000) * 1_000).toISOString();
}

export function leftScope(right: Scope, left: LeftSide): Scope {
  return { project: right.project, environment: left.environment, cluster: left.cluster };
}

export function sameScope(a: Scope, b: Scope): boolean {
  return a.project === b.project && a.environment === b.environment && a.cluster === b.cluster;
}

export function sideLabel(scope: Scope, at: string | undefined, mode: DriftMode): string {
  if (mode === "environments") {
    return `${scope.environment} · ${scope.cluster}`;
  }
  return `${scope.environment} · ${at === undefined ? "now" : formatInstant(at)}`;
}

export function sideLabels(right: Scope, state: DriftState): SideLabels | undefined {
  if (state.mode === "environments") {
    return state.left === undefined
      ? undefined
      : {
          left: sideLabel(leftScope(right, state.left), state.at, "environments"),
          right: sideLabel(right, state.at, "environments"),
        };
  }
  return state.from === undefined
    ? undefined
    : {
        left: sideLabel(right, state.from, "timeline"),
        right: sideLabel(right, state.to, "timeline"),
      };
}

export interface Counters {
  readonly nodesAdded: number;
  readonly nodesRemoved: number;
  readonly nodesChanged: number;
  readonly edgesAdded: number;
  readonly edgesRemoved: number;
}

export function counters(diff: TopologyDiff): Counters {
  return {
    nodesAdded: diff.nodesAdded.length,
    nodesRemoved: diff.nodesRemoved.length,
    nodesChanged: diff.nodesChanged.length,
    edgesAdded: diff.edgesAdded.length,
    edgesRemoved: diff.edgesRemoved.length,
  };
}

export function totalDifferences(diff: TopologyDiff): number {
  const c = counters(diff);
  return c.nodesAdded + c.nodesRemoved + c.nodesChanged + c.edgesAdded + c.edgesRemoved;
}

export interface DiffRow {
  readonly kind: ChangeKind;
  readonly key: string;
  readonly name: string;
  readonly detail: string;
  readonly nodeId: string;
}

export interface DiffGroup {
  readonly id: string;
  readonly label: string;
  readonly rows: readonly DiffRow[];
  readonly summary: string;
}

export function diffGroups(
  diff: TopologyDiff,
  sides: SideLabels,
  graph?: TopologyGraph,
): DiffGroup[] {
  const names = knownNames(diff, graph);
  const nodeRows: DiffRow[] = [
    ...diff.nodesAdded.map((node) => ({
      kind: "added" as const,
      key: node.id,
      name: node.name,
      detail: `exists only in ${sides.right}${versionSuffix(node.versions)}`,
      nodeId: node.id,
    })),
    ...diff.nodesRemoved.map((node) => ({
      kind: "removed" as const,
      key: node.id,
      name: node.name,
      detail: `exists only in ${sides.left}${versionSuffix(node.versions)}`,
      nodeId: node.id,
    })),
    ...diff.nodesChanged.map((change) => ({
      kind: "changed" as const,
      key: change.id,
      name: change.name,
      detail: describeChange(change, sides),
      nodeId: change.id,
    })),
  ];
  const typeOf = new Map<string, NodeType>([
    ...diff.nodesAdded.map((node) => [node.id, node.type] as const),
    ...diff.nodesRemoved.map((node) => [node.id, node.type] as const),
    ...diff.nodesChanged.map((change) => [change.id, change.type] as const),
  ]);
  const byType = NODE_TYPES.map((type) =>
    group(
      type.toLowerCase(),
      NODE_TYPE_LABELS[type].plural,
      nodeRows.filter((row) => typeOf.get(row.nodeId) === type),
    ),
  );
  const edgeRows: DiffRow[] = [
    ...diff.edgesAdded.map((edge) => edgeRow(edge, "added", sides.right, names)),
    ...diff.edgesRemoved.map((edge) => edgeRow(edge, "removed", sides.left, names)),
  ];
  return [...byType, group("dependencies", "Dependencies", edgeRows)];
}

function group(id: string, label: string, rows: readonly DiffRow[]): DiffGroup {
  const count = (kind: ChangeKind) => rows.filter((row) => row.kind === kind).length;
  const summary =
    rows.length === 0
      ? "identical"
      : `${String(count("added"))} added · ${String(count("removed"))} removed · ${String(count("changed"))} changed`;
  return { id, label, rows, summary };
}

function edgeRow(
  edge: EdgeRef,
  kind: EdgeChangeKind,
  side: string,
  names: ReadonlyMap<string, string>,
): DiffRow {
  return {
    kind,
    key: edgeKey(edge),
    name: `${nameOf(edge.sourceId, names)} → ${nameOf(edge.targetId, names)}`,
    detail: `${edge.kind.toLowerCase()} · only in ${side}`,
    nodeId: edge.targetId,
  };
}

export function edgeKey(edge: EdgeRef): string {
  return `${edge.sourceId}>${edge.targetId}:${edge.kind}`;
}

function knownNames(diff: TopologyDiff, graph?: TopologyGraph): ReadonlyMap<string, string> {
  return new Map([
    ...(graph?.nodes ?? []).map((node) => [node.id, node.name] as const),
    ...diff.nodesAdded.map((node) => [node.id, node.name] as const),
    ...diff.nodesRemoved.map((node) => [node.id, node.name] as const),
    ...diff.nodesChanged.map((change) => [change.id, change.name] as const),
  ]);
}

export function nameOf(id: string, names: ReadonlyMap<string, string>): string {
  return names.get(id) ?? id.slice(id.indexOf(":") + 1);
}

function versionSuffix(versions: readonly string[]): string {
  return versions.length === 0 ? "" : ` · ${formatVersions(versions)}`;
}

function formatVersions(versions: readonly string[]): string {
  return versions.length === 0 ? "no version" : versions.map((version) => `v${version}`).join(", ");
}

function formatDeployments(deployments: NodeChange["deploymentsBefore"]): string {
  return deployments.length === 0
    ? "nowhere"
    : deployments
        .map((deployment) =>
          deployment.namespace === ""
            ? deployment.cluster
            : `${deployment.cluster}/${deployment.namespace}`,
        )
        .join(", ");
}

export function describeChange(change: NodeChange, sides: SideLabels): string {
  const versionsDiffer = !sameSet(change.versionsBefore, change.versionsAfter);
  if (versionsDiffer) {
    return `${formatVersions(change.versionsBefore)} in ${sides.left} → ${formatVersions(change.versionsAfter)} in ${sides.right}`;
  }
  return `deployed at ${formatDeployments(change.deploymentsBefore)} → ${formatDeployments(change.deploymentsAfter)}`;
}

export function versionArrow(change: NodeChange): string {
  return `${formatVersions(change.versionsBefore)} → ${formatVersions(change.versionsAfter)}`;
}

function sameSet(a: readonly string[], b: readonly string[]): boolean {
  return a.length === b.length && a.every((value) => b.includes(value));
}

export function overlay(diff: TopologyDiff): DriftOverlay {
  return {
    nodes: new Map([
      ...diff.nodesAdded.map((node) => [node.id, "added"] as const),
      ...diff.nodesRemoved.map((node) => [node.id, "removed"] as const),
      ...diff.nodesChanged.map((change) => [change.id, "changed"] as const),
    ]),
    edges: new Map([
      ...diff.edgesAdded.map((edge) => [edgeKey(edge), "added"] as const),
      ...diff.edgesRemoved.map((edge) => [edgeKey(edge), "removed"] as const),
    ]),
    subtitles: new Map(diff.nodesChanged.map((change) => [change.id, versionArrow(change)])),
  };
}

const NO_METRICS: TopologyEdge["metrics"] = {
  calls: 0,
  errors: 0,
  p50Millis: 0,
  p95Millis: 0,
  p99Millis: 0,
  maxMillis: 0,
};

export function unionGraph(right: TopologyGraph, diff: TopologyDiff): VisibleGraph {
  const present = new Set(right.nodes.map((node) => node.id));
  const nodes = [...right.nodes, ...diff.nodesRemoved.filter((node) => !present.has(node.id))];
  const ids = new Set(nodes.map((node) => node.id));
  const drawn = new Set(right.edges.map(edgeId));
  const ghosts: TopologyEdge[] = diff.edgesRemoved
    .filter((edge) => !drawn.has(edgeKey(edge)) && ids.has(edge.sourceId) && ids.has(edge.targetId))
    .map((edge) => ({ ...edge, metrics: NO_METRICS }));
  return { nodes, edges: [...right.edges, ...ghosts] };
}

export interface Sentence {
  readonly kind: ChangeKind | "aligned";
  readonly badge: string;
  readonly title: string;
  readonly body: string;
}

export function summarise(
  diff: TopologyDiff,
  sides: SideLabels,
  graph?: TopologyGraph,
): Sentence[] {
  const names = knownNames(diff, graph);
  const list = (nodes: readonly TopologyNode[]) => listNames(nodes.map((node) => node.name));
  const describeEdge = (edge: EdgeRef) =>
    `${nameOf(edge.sourceId, names)} → ${nameOf(edge.targetId, names)}`;
  const sentences: Sentence[] = [];
  if (diff.nodesAdded.length > 0) {
    const touching = diff.edgesAdded.filter((edge) =>
      diff.nodesAdded.some((node) => node.id === edge.sourceId || node.id === edge.targetId),
    );
    sentences.push({
      kind: "added",
      badge: `+${String(diff.nodesAdded.length)}`,
      title: `${list(diff.nodesAdded)} ${diff.nodesAdded.length > 1 ? "exist" : "exists"} only in ${sides.right}`,
      body:
        touching.length === 0
          ? "No dependency touches them yet."
          : `Brings ${touching.map(describeEdge).join(", ")}.`,
    });
  }
  if (diff.nodesRemoved.length > 0) {
    sentences.push({
      kind: "removed",
      badge: `−${String(diff.nodesRemoved.length)}`,
      title: `${list(diff.nodesRemoved)} missing from ${sides.right}`,
      body: `Present in ${sides.left} only. A rename shows as removed plus added, which is the truthful answer.`,
    });
  }
  if (diff.nodesChanged.length > 0) {
    sentences.push({
      kind: "changed",
      badge: `Δ${String(diff.nodesChanged.length)}`,
      title: `${plural(diff.nodesChanged.length, "node")} differ${diff.nodesChanged.length > 1 ? "" : "s"} between the sides`,
      body: `${diff.nodesChanged.map((change) => `${change.name} ${describeChange(change, sides)}`).join("; ")}.`,
    });
  }
  const edgeCount = diff.edgesAdded.length + diff.edgesRemoved.length;
  if (edgeCount > 0) {
    sentences.push({
      kind: diff.edgesAdded.length > 0 ? "added" : "removed",
      badge: String(edgeCount),
      title: `${plural(edgeCount, "dependency", "dependencies")} differ${edgeCount > 1 ? "" : "s"}`,
      body: listNames(
        [
          ...diff.edgesAdded.map((edge) => `${describeEdge(edge)} only in ${sides.right}`),
          ...diff.edgesRemoved.map((edge) => `${describeEdge(edge)} only in ${sides.left}`),
        ],
        "; ",
      ),
    });
  }
  if (sentences.length === 0) {
    sentences.push({
      kind: "aligned",
      badge: "aligned",
      title: "Both sides are identical",
      body: "Same node ids, versions and dependency keys. Metrics may still differ; they are not part of drift.",
    });
  }
  return sentences;
}

const NAMES_SHOWN = 5;

export function listNames(names: readonly string[], separator = ", "): string {
  if (names.length <= NAMES_SHOWN) {
    return names.join(separator);
  }
  const rest = names.length - NAMES_SHOWN;
  return `${names.slice(0, NAMES_SHOWN).join(separator)} and ${String(rest)} more`;
}

export interface SelectedNode {
  readonly id: string;
  readonly name: string;
  readonly kind: ChangeKind | "same";
  readonly detail: string;
}

export function describeSelected(
  diff: TopologyDiff,
  id: string,
  sides: SideLabels,
  graph: TopologyGraph | undefined,
): SelectedNode | undefined {
  const added = diff.nodesAdded.find((node) => node.id === id);
  if (added !== undefined) {
    return { id, name: added.name, kind: "added", detail: `Exists only in ${sides.right}.` };
  }
  const removed = diff.nodesRemoved.find((node) => node.id === id);
  if (removed !== undefined) {
    return { id, name: removed.name, kind: "removed", detail: `Exists only in ${sides.left}.` };
  }
  const changed = diff.nodesChanged.find((change) => change.id === id);
  if (changed !== undefined) {
    return { id, name: changed.name, kind: "changed", detail: describeChange(changed, sides) };
  }
  const node = graph?.nodes.find((candidate) => candidate.id === id);
  return node === undefined
    ? undefined
    : { id, name: node.name, kind: "same", detail: "Identical on both sides." };
}

export function headline(diff: TopologyDiff): string {
  const total = totalDifferences(diff);
  if (total === 0) {
    return "No differences";
  }
  const c = counters(diff);
  return `${plural(total, "difference")} · ${String(c.nodesAdded)} + ${String(c.nodesRemoved)} nodes, ${String(c.nodesChanged)} changed, ${String(c.edgesAdded + c.edgesRemoved)} dependencies`;
}
