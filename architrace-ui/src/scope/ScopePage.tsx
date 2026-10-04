/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { useQuery } from "@tanstack/react-query";
import { Link, useParams, useSearchParams } from "react-router";
import { describeError, type TopologyGraph } from "../api/client";
import { graphQuery } from "../api/queries";
import { Shell } from "../app/Shell";
import { ContextPanel } from "../map/ContextPanel";
import {
  countByType,
  describeGraph,
  describeMatches,
  type MapState,
  parseMapState,
  type Selection,
  toggleNodeType,
  toMapParams,
  visibleGraph,
} from "../map/model";
import { NodeTypeChips } from "../map/NodeTypeChips";
import { ServiceMap } from "../map/ServiceMap";
import { TimeSelector } from "../map/TimeSelector";
import { scopePath } from "../projects/filters";

export function ScopePage() {
  const { project = "", environment = "", cluster = "" } = useParams();
  const scope = { project, environment, cluster };
  const path = scopePath(scope);
  const [params, setParams] = useSearchParams();
  const state = parseMapState(params);
  const graph = useQuery(graphQuery(scope, state.at));
  const update = (next: MapState) => {
    setParams(toMapParams(next), { replace: true });
  };
  const select = (selection: Selection | undefined) => {
    update({ ...state, selection });
  };
  const request = `/api/v1${path}/graph${state.at === undefined ? "" : `?at=${encodeURIComponent(state.at)}`}`;

  const tools = (
    <>
      <nav className="breadcrumb" aria-label="Scope">
        <Link to="/">Projects</Link>
        <span aria-hidden="true">/</span>
        <span>{project}</span>
        <span aria-hidden="true">/</span>
        <span className="badge b-kind">{environment}</span>
        <span aria-hidden="true">/</span>
        <span className="mono">{cluster}</span>
      </nav>
      <input
        className="ask-input"
        type="search"
        aria-label="Find in map"
        placeholder="Find a service, data store or data stream…"
        value={state.query}
        onChange={(event) => {
          update({ ...state, query: event.target.value });
        }}
      />
      <TimeSelector
        at={state.at}
        onChange={(at) => {
          update({ ...state, at });
        }}
      />
    </>
  );
  const aside =
    graph.isSuccess && graph.data.nodes.length > 0 ? (
      <ContextPanel graph={graph.data} state={state} onSelect={select} />
    ) : undefined;

  return (
    <Shell title="Service map" tools={tools} apiRequest={request} mapPath={path} aside={aside}>
      {graph.isPending && <output className="status">Loading the graph…</output>}
      {graph.isError && (
        <p className="status status-error" role="alert">
          The control plane did not answer: {describeError(graph.error)}
        </p>
      )}
      {graph.isSuccess && (
        <MapView graph={graph.data} state={state} onChange={update} onSelect={select} />
      )}
    </Shell>
  );
}

interface MapViewProps {
  readonly graph: TopologyGraph;
  readonly state: MapState;
  readonly onChange: (state: MapState) => void;
  readonly onSelect: (selection: Selection | undefined) => void;
}

function MapView({ graph, state, onChange, onSelect }: MapViewProps) {
  if (graph.nodes.length === 0) {
    return (
      <p className="empty">
        No snapshot has reached the control plane for this scope yet. Start an agent for it and the
        map draws itself.
      </p>
    );
  }
  const visible = visibleGraph(graph, state);
  return (
    <>
      <div className="toolbar">
        <NodeTypeChips
          counts={countByType(graph.nodes)}
          filter={state}
          onToggle={(type) => {
            onChange(toggleNodeType(state, type));
          }}
        />
        <fieldset className="seg">
          <legend className="seg-label">Lens</legend>
          <button
            type="button"
            aria-pressed={state.lens === "all"}
            onClick={() => {
              onChange({ ...state, lens: "all" });
            }}
          >
            All
          </button>
          <button
            type="button"
            aria-pressed={state.lens === "streams"}
            onClick={() => {
              onChange({ ...state, lens: "streams" });
            }}
          >
            Data streams
          </button>
        </fieldset>
        <output className="count-line toolbar-right">
          {state.query.trim() === ""
            ? describeGraph(visible)
            : describeMatches(visible, state.query)}
        </output>
      </div>
      {visible.nodes.length === 0 ? (
        <p className="empty">Every node type is hidden. Switch one back on to see the map.</p>
      ) : (
        <ServiceMap graph={visible} state={state} onSelect={onSelect} />
      )}
    </>
  );
}
