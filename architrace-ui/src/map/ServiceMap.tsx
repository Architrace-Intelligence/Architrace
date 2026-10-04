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
import {
  type EdgeHealth,
  edgeHealth,
  edgeId,
  edgeWidth,
  NODE_TYPE_TOKENS,
  type VisibleGraph,
} from "./model";
import { type CardNode, NodeCard } from "./NodeCard";

const CARD_TYPES = { card: NodeCard };
const HEALTHS: readonly EdgeHealth[] = ["ok", "warn", "bad"];

interface ServiceMapProps {
  readonly graph: VisibleGraph;
}

export function ServiceMap({ graph }: ServiceMapProps) {
  const layout = useQuery({
    queryKey: ["layout", graph.nodes.map((node) => node.id), graph.edges.map(edgeId)],
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
  return <MapCanvas graph={graph} positions={layout.data} />;
}

interface MapCanvasProps {
  readonly graph: VisibleGraph;
  readonly positions: Positions;
}

function MapCanvas({ graph, positions }: MapCanvasProps) {
  const nodes = useMemo<CardNode[]>(
    () =>
      graph.nodes.map((node) => ({
        id: node.id,
        type: "card",
        position: positions.get(node.id) ?? { x: 0, y: 0 },
        width: NODE_WIDTH,
        height: NODE_HEIGHT,
        data: { node },
      })),
    [graph, positions],
  );
  const edges = useMemo<Edge[]>(
    () =>
      graph.edges.map((edge) => {
        const health = edgeHealth(edge.metrics);
        return {
          id: edgeId(edge),
          source: edge.sourceId,
          target: edge.targetId,
          className: `edge-${edge.kind.toLowerCase()} health-${health}`,
          style: { strokeWidth: edgeWidth(edge.metrics.calls) },
          markerEnd: `arrow-${health}`,
        };
      }),
    [graph],
  );
  return (
    <section className="map" aria-label="Service map canvas">
      <svg className="map-markers" aria-hidden="true">
        <defs>
          {HEALTHS.map((health) => (
            <marker
              key={health}
              id={`arrow-${health}`}
              className={`arrow-${health}`}
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
        elementsSelectable={false}
        edgesFocusable={false}
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
          <MapLegend />
        </Panel>
      </ReactFlow>
    </section>
  );
}
