/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { useQuery } from "@tanstack/react-query";
import { useNavigate, useParams, useSearchParams } from "react-router";
import { describeError, type Finding, type Impact, type TopologyGraph } from "../api/client";
import { findingsQuery, graphQuery, impactQuery, scopesQuery } from "../api/queries";
import { Shell } from "../app/Shell";
import { findingsByNode } from "../findings/model";
import { ContextPanel } from "../map/ContextPanel";
import {
  countByType,
  describeGraph,
  describeMatches,
  graphNamespaces,
  impactOverlay,
  type Lens,
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
import { ScopeSwitcher } from "./ScopeSwitcher";

const LENS_LABELS: Record<Lens, string> = {
  all: "All",
  streams: "Data streams",
  impact: "Impact",
};

function apiRequest(path: string, state: MapState, subject: string | undefined): string {
  const params = new URLSearchParams();
  if (subject !== undefined) {
    params.set("node", subject);
  }
  if (state.at !== undefined) {
    params.set("at", state.at);
  }
  const query = params.toString();
  const resource = subject === undefined ? "graph" : "impact";
  return `/api/v1${path}/${resource}${query === "" ? "" : `?${query}`}`;
}

function describeImpact(
  subject: string | undefined,
  pending: boolean,
  error: Error | null,
): string | undefined {
  if (subject === undefined) {
    return undefined;
  }
  if (error !== null) {
    return `The control plane did not answer: ${describeError(error)}`;
  }
  return pending ? "Computing what breaks if it fails…" : undefined;
}

export function ScopePage() {
  const { project = "", environment = "", cluster = "" } = useParams();
  const scope = { project, environment, cluster };
  const path = scopePath(scope);
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();
  const state = parseMapState(params);
  const graph = useQuery(graphQuery(scope, state.at));
  const scopes = useQuery(scopesQuery());
  const findings = useQuery({ ...findingsQuery(scope), enabled: state.at === undefined });
  const current = state.at === undefined ? findings.data : undefined;
  const subject =
    state.lens === "impact" && state.selection?.kind === "node" ? state.selection.id : undefined;
  const impact = useQuery({
    ...impactQuery(scope, subject ?? "", state.at),
    enabled: subject !== undefined,
  });
  const blast =
    subject !== undefined && impact.data?.subject.id === subject ? impact.data : undefined;
  const update = (next: MapState) => {
    setParams(toMapParams(next), { replace: true });
  };
  const select = (selection: Selection | undefined) => {
    update({ ...state, selection });
  };
  const request = apiRequest(path, state, subject);

  const tools = (
    <>
      <ScopeSwitcher
        current={scope}
        scopes={scopes.data?.map((summary) => summary.scope) ?? []}
        onSwitch={(target) => {
          void navigate({ pathname: scopePath(target), search: params.toString() });
        }}
      />
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
      <ContextPanel
        graph={graph.data}
        state={state}
        onSelect={select}
        findings={current}
        impact={blast}
        onImpact={() => {
          update({ ...state, lens: "impact" });
        }}
      />
    ) : undefined;

  return (
    <Shell title="Service map" tools={tools} apiRequest={request} scopePath={path} aside={aside}>
      {graph.isPending && <output className="status">Loading the graph…</output>}
      {graph.isError && (
        <p className="status status-error" role="alert">
          The control plane did not answer: {describeError(graph.error)}
        </p>
      )}
      {graph.isSuccess && (
        <MapView
          graph={graph.data}
          state={state}
          findings={current}
          impact={blast}
          impactStatus={describeImpact(subject, impact.isPending, impact.error)}
          onChange={update}
          onSelect={select}
        />
      )}
    </Shell>
  );
}

interface MapViewProps {
  readonly graph: TopologyGraph;
  readonly state: MapState;
  readonly findings: readonly Finding[] | undefined;
  readonly impact: Impact | undefined;
  readonly impactStatus: string | undefined;
  readonly onChange: (state: MapState) => void;
  readonly onSelect: (selection: Selection | undefined) => void;
}

function MapView({
  graph,
  state,
  findings,
  impact,
  impactStatus,
  onChange,
  onSelect,
}: MapViewProps) {
  if (graph.nodes.length === 0) {
    return (
      <p className="empty">
        No snapshot has reached the control plane for this scope yet. Start an agent for it and the
        map draws itself.
      </p>
    );
  }
  const visible = visibleGraph(graph, state);
  const namespaces = graphNamespaces(graph);
  return (
    <>
      <div className="toolbar">
        {namespaces.length > 0 && (
          <label className="select-wrap">
            <span className="sr-only">Namespace</span>
            <select
              className="chip-select"
              value={state.namespace ?? ""}
              onChange={(event) => {
                onChange({
                  ...state,
                  namespace: event.target.value === "" ? undefined : event.target.value,
                });
              }}
            >
              <option value="">All namespaces</option>
              {namespaces.map((namespace) => (
                <option key={namespace} value={namespace}>
                  {namespace}
                </option>
              ))}
            </select>
          </label>
        )}
        <NodeTypeChips
          counts={countByType(graph.nodes)}
          filter={state}
          onToggle={(type) => {
            onChange(toggleNodeType(state, type));
          }}
        />
        <fieldset className="seg">
          <legend className="seg-label">Lens</legend>
          {(Object.keys(LENS_LABELS) as Lens[]).map((lens) => (
            <button
              key={lens}
              type="button"
              aria-pressed={state.lens === lens}
              onClick={() => {
                onChange({ ...state, lens });
              }}
            >
              {LENS_LABELS[lens]}
            </button>
          ))}
        </fieldset>
        {state.lens === "impact" && state.selection?.kind !== "node" && (
          <span className="count-line semantics">
            Select a node to see what breaks if it fails.
          </span>
        )}
        <output className="count-line toolbar-right">
          {impactStatus ??
            (state.query.trim() === ""
              ? describeGraph(visible)
              : describeMatches(visible, state.query))}
        </output>
      </div>
      {visible.nodes.length === 0 ? (
        <p className="empty">Every node type is hidden. Switch one back on to see the map.</p>
      ) : (
        <ServiceMap
          graph={visible}
          state={state}
          onSelect={onSelect}
          findings={findings === undefined ? undefined : findingsByNode(findings)}
          impact={impact === undefined ? undefined : impactOverlay(impact)}
        />
      )}
    </>
  );
}
