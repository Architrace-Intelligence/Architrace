/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { useQuery } from "@tanstack/react-query";
import { Link, useParams, useSearchParams } from "react-router";
import { describeError, type TopologyGraph } from "../api/client";
import { graphQuery } from "../api/queries";
import { Shell } from "../app/Shell";
import {
  countByType,
  describeGraph,
  type MapFilter,
  parseMapFilter,
  toggleNodeType,
  toMapParams,
  visibleGraph,
} from "../map/model";
import { NodeTypeChips } from "../map/NodeTypeChips";
import { ServiceMap } from "../map/ServiceMap";
import { scopePath } from "../projects/filters";

export function ScopePage() {
  const { project = "", environment = "", cluster = "" } = useParams();
  const scope = { project, environment, cluster };
  const path = scopePath(scope);
  const [params, setParams] = useSearchParams();
  const filter = parseMapFilter(params);
  const graph = useQuery(graphQuery(scope));

  const crumbs = (
    <nav className="breadcrumb" aria-label="Scope">
      <Link to="/">Projects</Link>
      <span aria-hidden="true">/</span>
      <span>{project}</span>
      <span aria-hidden="true">/</span>
      <span className="badge b-kind">{environment}</span>
      <span aria-hidden="true">/</span>
      <span className="mono">{cluster}</span>
    </nav>
  );

  return (
    <Shell title="Service map" tools={crumbs} apiRequest={`/api/v1${path}/graph`} mapPath={path}>
      {graph.isPending && <output className="status">Loading the graph…</output>}
      {graph.isError && (
        <p className="status status-error" role="alert">
          The control plane did not answer: {describeError(graph.error)}
        </p>
      )}
      {graph.isSuccess && (
        <MapView
          graph={graph.data}
          filter={filter}
          onChange={(next) => {
            setParams(toMapParams(next), { replace: true });
          }}
        />
      )}
    </Shell>
  );
}

interface MapViewProps {
  readonly graph: TopologyGraph;
  readonly filter: MapFilter;
  readonly onChange: (filter: MapFilter) => void;
}

function MapView({ graph, filter, onChange }: MapViewProps) {
  if (graph.nodes.length === 0) {
    return (
      <p className="empty">
        No snapshot has reached the control plane for this scope yet. Start an agent for it and the
        map draws itself.
      </p>
    );
  }
  const visible = visibleGraph(graph, filter);
  return (
    <>
      <div className="toolbar">
        <NodeTypeChips
          counts={countByType(graph.nodes)}
          filter={filter}
          onToggle={(type) => {
            onChange(toggleNodeType(filter, type));
          }}
        />
        <output className="count-line toolbar-right">{describeGraph(visible)}</output>
      </div>
      {visible.nodes.length === 0 ? (
        <p className="empty">Every node type is hidden. Switch one back on to see the map.</p>
      ) : (
        <ServiceMap graph={visible} />
      )}
    </>
  );
}
