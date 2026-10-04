/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { useQuery } from "@tanstack/react-query";
import {
  Background,
  BackgroundVariant,
  Controls,
  type Edge,
  MiniMap,
  Panel,
  ReactFlow,
} from "@xyflow/react";
import "@xyflow/react/dist/style.css";
import { useMemo } from "react";
import { plural } from "../projects/format";
import { layoutGraph, NODE_HEIGHT, NODE_WIDTH, type Positions } from "./layout";
import { MapLegend } from "./MapLegend";
import { describeMetrics } from "./metrics";
import {
  type DriftOverlay,
  type EdgeChangeKind,
  type EdgeHealth,
  edgeHealth,
  edgeId,
  edgeWidth,
  looks,
  type MapState,
  NODE_TYPE_TOKENS,
  type Selection,
  type SideLabels,
  type VisibleGraph,
} from "./model";
import { type CardNode, NodeCard } from "./NodeCard";

const CARD_TYPES = { card: NodeCard };
const MARKERS: readonly (EdgeHealth | "touch" | EdgeChangeKind)[] = [
  "ok",
  "warn",
  "bad",
  "touch",
  "added",
  "removed",
];

interface ServiceMapProps {
  readonly graph: VisibleGraph;
  readonly state: MapState;
  readonly onSelect: (selection: Selection | undefined) => void;
  readonly overlay?: DriftOverlay;
  readonly sides?: SideLabels;
}

export function ServiceMap({ graph, state, onSelect, overlay, sides }: ServiceMapProps) {
  const key = [...graph.nodes.map((node) => node.id), ...graph.edges.map(edgeId)].join("|");
  const layout = useQuery({
    queryKey: ["layout", key],
    queryFn: () => layoutGraph(graph.nodes, graph.edges),
    staleTime: Infinity,
  });
  if (layout.isPending) {
    return <output className="status">Laying out {plural(graph.nodes.length, "node")}…</output>;
  }
  if (layout.isError) {
    return (
      <p className="status status-error" role="alert">
        The layout failed: {layout.error.message}
      </p>
    );
  }
  return (
    <MapCanvas
      key={key}
      graph={graph}
      positions={layout.data}
      state={state}
      onSelect={onSelect}
      overlay={overlay}
      sides={sides}
    />
  );
}

interface MapCanvasProps {
  readonly graph: VisibleGraph;
  readonly positions: Positions;
  readonly state: MapState;
  readonly onSelect: (selection: Selection | undefined) => void;
  readonly overlay: DriftOverlay | undefined;
  readonly sides: SideLabels | undefined;
}

function MapCanvas({ graph, positions, state, onSelect, overlay, sides }: MapCanvasProps) {
  const look = useMemo(() => looks(graph, state), [graph, state]);
  const nodes = useMemo<CardNode[]>(
    () =>
      graph.nodes.map((node) => {
        const nodeLook = look.nodes.get(node.id);
        const change = overlay?.nodes.get(node.id);
        return {
          id: node.id,
          type: "card",
          position: positions.get(node.id) ?? { x: 0, y: 0 },
          width: NODE_WIDTH,
          height: NODE_HEIGHT,
          data: { node, onSelect, change, subtitle: overlay?.subtitles.get(node.id) },
          selected: nodeLook?.selected ?? false,
          className: classNames([
            [nodeLook?.dimmed ?? false, "dimmed"],
            [nodeLook?.match ?? false, "match"],
            [change !== undefined, `drift-${change ?? ""}`],
          ]),
        };
      }),
    [graph, positions, look, onSelect, overlay],
  );
  const edges = useMemo<Edge[]>(
    () =>
      graph.edges.map((edge) => {
        const id = edgeId(edge);
        const health = edgeHealth(edge.metrics);
        const edgeLook = look.edges.get(id);
        const touching = edgeLook?.touching ?? false;
        const change = overlay?.edges.get(id);
        const marker = change ?? (touching ? "touch" : health);
        return {
          id,
          source: edge.sourceId,
          target: edge.targetId,
          className: classNames([
            [true, `edge-${edge.kind.toLowerCase()}`],
            [true, `health-${health}`],
            [touching, "touching"],
            [edgeLook?.dimmed ?? false, "dimmed"],
            [change !== undefined, `drift-${change ?? ""}`],
          ]),
          style: { strokeWidth: edgeWidth(edge.metrics.calls) },
          markerEnd: `arrow-${marker}`,
          label: touching && overlay === undefined ? describeMetrics(edge.metrics) : undefined,
          labelStyle: { fill: "var(--ink)", fontSize: 11 },
          labelBgStyle: { fill: "var(--bg-2)", stroke: "var(--accent)" },
          labelBgPadding: [6, 3],
          labelBgBorderRadius: 11,
        };
      }),
    [graph, look, overlay],
  );
  return (
    <section className="map" aria-label="Service map canvas">
      <svg className="map-markers" aria-hidden="true">
        <defs>
          {MARKERS.map((marker) => (
            <marker
              key={marker}
              id={`arrow-${marker}`}
              className={`arrow-${marker}`}
              viewBox="0 0 10 10"
              refX="9"
              refY="5"
              markerWidth="9"
              markerHeight="9"
              markerUnits="userSpaceOnUse"
              orient="auto-start-reverse"
            >
              <path d="M0 0L10 5 0 10z" />
            </marker>
          ))}
        </defs>
      </svg>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        nodeTypes={CARD_TYPES}
        fitView
        fitViewOptions={{ padding: 0.08 }}
        minZoom={0.02}
        maxZoom={2}
        nodesDraggable={false}
        nodesConnectable={false}
        nodesFocusable={false}
        elementsSelectable={false}
        edgesFocusable={false}
        onNodeClick={(_, node) => {
          onSelect({ kind: "node", id: node.id });
        }}
        onEdgeClick={(_, edge) => {
          onSelect({ kind: "edge", id: edge.id });
        }}
        onPaneClick={() => {
          onSelect(undefined);
        }}
      >
        <Background variant={BackgroundVariant.Dots} gap={24} size={1} />
        <Controls position="top-right" showInteractive={false} />
        <MiniMap<CardNode>
          position="bottom-right"
          pannable
          zoomable
          nodeClassName={(node) => `mini-${NODE_TYPE_TOKENS[node.data.node.type]}`}
        />
        <Panel position="bottom-left">
          <MapLegend sides={sides} />
        </Panel>
      </ReactFlow>
    </section>
  );
}

function classNames(flags: readonly (readonly [boolean, string])[]): string {
  return flags
    .filter(([enabled]) => enabled)
    .map(([, name]) => name)
    .join(" ");
}
