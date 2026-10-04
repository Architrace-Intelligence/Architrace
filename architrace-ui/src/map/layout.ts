/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import type { ELK, LayoutOptions } from "elkjs/lib/elk-api";
import type { TopologyEdge, TopologyNode } from "../api/client";
import { edgeId } from "./model";

export const NODE_WIDTH = 192;
export const NODE_HEIGHT = 56;

export interface Point {
  readonly x: number;
  readonly y: number;
}

export type Positions = ReadonlyMap<string, Point>;

const LAYOUT_OPTIONS: LayoutOptions = {
  "elk.algorithm": "layered",
  "elk.direction": "RIGHT",
  "elk.layered.spacing.nodeNodeBetweenLayers": "96",
  "elk.spacing.nodeNode": "24",
  "elk.layered.thoroughness": "3",
};

let engine: Promise<ELK> | undefined;

function elk(): Promise<ELK> {
  engine ??= import("elkjs/lib/elk.bundled.js").then(({ default: Elk }) => new Elk());
  return engine;
}

export async function layoutGraph(
  nodes: readonly TopologyNode[],
  edges: readonly TopologyEdge[],
): Promise<Positions> {
  const result = await (
    await elk()
  ).layout({
    id: "root",
    layoutOptions: LAYOUT_OPTIONS,
    children: nodes.map((node) => ({ id: node.id, width: NODE_WIDTH, height: NODE_HEIGHT })),
    edges: edges.map((edge) => ({
      id: edgeId(edge),
      sources: [edge.sourceId],
      targets: [edge.targetId],
    })),
  });
  return new Map(
    (result.children ?? []).map((child) => [child.id, { x: child.x ?? 0, y: child.y ?? 0 }]),
  );
}
