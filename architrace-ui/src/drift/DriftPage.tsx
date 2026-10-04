/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { type UseQueryResult, useQuery } from "@tanstack/react-query";
import { Navigate, useNavigate, useParams, useSearchParams } from "react-router";
import { describeError, type Scope, type TopologyDiff, type TopologyGraph } from "../api/client";
import { environmentDiffQuery, graphQuery, scopesQuery, timelineDiffQuery } from "../api/queries";
import { Shell } from "../app/Shell";
import { GLYPHS, INITIAL_MAP_STATE, type SideLabels } from "../map/model";
import { ServiceMap } from "../map/ServiceMap";
import { TimeSelector } from "../map/TimeSelector";
import { scopePath } from "../projects/filters";
import { ScopeSwitcher } from "../scope/ScopeSwitcher";
import { DriftList } from "./DriftList";
import { DriftPanel } from "./DriftPanel";
import {
  counters,
  defaultFrom,
  defaultLeft,
  diffGroups,
  type DriftMode,
  type DriftState,
  type DriftView,
  INITIAL_DRIFT_STATE,
  isReady,
  leftScope,
  otherScopes,
  overlay,
  parseDriftState,
  sameScope,
  sideLabels,
  toDriftParams,
  unionGraph,
} from "./model";

const MODE_LABELS: Record<DriftMode, string> = {
  environments: "Environments",
  timeline: "Timeline",
};

const VIEW_LABELS: Record<DriftView, string> = { list: "List", map: "Map" };

export function DriftPage() {
  const { project = "", environment = "", cluster = "" } = useParams();
  const right: Scope = { project, environment, cluster };
  const path = scopePath(right);
  const driftPath = `${path}/drift`;
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();
  const state = parseDriftState(params);
  const scopes = useQuery(scopesQuery());
  const others = otherScopes(scopes.data?.map((summary) => summary.scope) ?? [], right);
  const ready = isReady(state);
  const diff = useQuery({
    ...(state.mode === "environments"
      ? environmentDiffQuery(
          right,
          state.left?.environment ?? "",
          state.left?.cluster ?? "",
          state.at,
        )
      : timelineDiffQuery(right, state.from ?? "", state.to)),
    enabled: ready,
  });
  const graph = useQuery({
    ...graphQuery(right, diff.data?.right.at),
    enabled: diff.isSuccess,
  });
  const update = (next: DriftState) => {
    setParams(toDriftParams(next), { replace: true });
  };
  const sides = sideLabels(right, state);

  if (state.mode === "environments" && state.left === undefined && others.length > 0) {
    const left = defaultLeft(others, right);
    return (
      <Navigate
        replace
        to={{ pathname: driftPath, search: toDriftParams({ ...state, left }).toString() }}
      />
    );
  }
  if (state.mode === "timeline" && state.from === undefined) {
    const from = defaultFrom(new Date());
    return (
      <Navigate
        replace
        to={{ pathname: driftPath, search: toDriftParams({ ...state, from }).toString() }}
      />
    );
  }

  const tools = (
    <>
      <ScopeSwitcher
        current={right}
        scopes={scopes.data?.map((summary) => summary.scope) ?? []}
        onSwitch={(target) => {
          const swapped =
            state.left !== undefined && sameScope(target, leftScope(right, state.left))
              ? { ...state, left: { environment: right.environment, cluster: right.cluster } }
              : state;
          void navigate({
            pathname: `${scopePath(target)}/drift`,
            search: toDriftParams(swapped).toString(),
          });
        }}
      />
      <fieldset className="seg">
        <legend className="seg-label">Mode</legend>
        {(Object.keys(MODE_LABELS) as DriftMode[]).map((mode) => (
          <button
            key={mode}
            type="button"
            aria-pressed={state.mode === mode}
            onClick={() => {
              update({ ...INITIAL_DRIFT_STATE, mode, view: state.view });
            }}
          >
            {MODE_LABELS[mode]}
          </button>
        ))}
      </fieldset>
    </>
  );
  const aside =
    diff.isSuccess && sides !== undefined ? (
      <DriftPanel
        diff={diff.data}
        sides={sides}
        selected={state.node}
        graph={graph.data}
        onSelect={(node) => {
          update({ ...state, node });
        }}
      />
    ) : undefined;
  return (
    <Shell
      title="Drift"
      tools={tools}
      apiRequest={apiRequest(path, state)}
      scopePath={path}
      aside={aside}
    >
      <div className="toolbar">
        {state.mode === "environments" ? (
          <EnvironmentSides
            right={right}
            others={others}
            state={state}
            onChange={update}
            onSwap={() => {
              if (state.left !== undefined) {
                const swappedLeft = { environment: right.environment, cluster: right.cluster };
                void navigate({
                  pathname: `${scopePath(leftScope(right, state.left))}/drift`,
                  search: toDriftParams({
                    ...state,
                    left: swappedLeft,
                    node: undefined,
                  }).toString(),
                });
              }
            }}
          />
        ) : (
          <TimelineSides right={right} state={state} onChange={update} />
        )}
        {sides !== undefined && (
          <span className="count-line semantics">
            Right side relative to left: “only in {sides.right}” is new on the right.
          </span>
        )}
        <fieldset className="seg toolbar-right">
          <legend className="seg-label">View</legend>
          {(Object.keys(VIEW_LABELS) as DriftView[]).map((view) => (
            <button
              key={view}
              type="button"
              aria-pressed={state.view === view}
              onClick={() => {
                update({ ...state, view });
              }}
            >
              {VIEW_LABELS[view]}
            </button>
          ))}
        </fieldset>
      </div>
      {scopes.isPending && !ready && <output className="status">Loading the scopes…</output>}
      {scopes.isError && !ready && (
        <p className="status status-error" role="alert">
          The control plane did not answer: {describeError(scopes.error)}
        </p>
      )}
      {state.mode === "environments" && scopes.isSuccess && others.length === 0 && (
        <p className="empty">
          {project} has only one scope, so there is no other environment to compare with. Switch to
          the Timeline mode to compare {environment} with its own past.
        </p>
      )}
      {ready && diff.isPending && <output className="status">Comparing both sides…</output>}
      {diff.isError && (
        <p className="status status-error" role="alert">
          The control plane did not answer: {describeError(diff.error)}
        </p>
      )}
      {diff.isSuccess && sides !== undefined && (
        <>
          <Counters diff={diff.data} sides={sides} />
          {state.view === "list" ? (
            <DriftList
              groups={diffGroups(diff.data, sides, graph.data)}
              onShowOnMap={(node) => {
                update({ ...state, view: "map", node });
              }}
            />
          ) : (
            <DriftMap
              diff={diff.data}
              sides={sides}
              state={state}
              graph={graph}
              onSelect={(node) => {
                update({ ...state, node });
              }}
            />
          )}
        </>
      )}
    </Shell>
  );
}

function apiRequest(path: string, state: DriftState): string {
  const query = new URLSearchParams();
  if (state.mode === "environments") {
    if (state.left !== undefined) {
      query.set("leftEnvironment", state.left.environment);
      query.set("leftCluster", state.left.cluster);
    }
    if (state.at !== undefined) {
      query.set("at", state.at);
    }
    return withQuery(`/api/v1${path}/diff/environments`, query);
  }
  if (state.from !== undefined) {
    query.set("from", state.from);
  }
  if (state.to !== undefined) {
    query.set("to", state.to);
  }
  return withQuery(`/api/v1${path}/diff/timeline`, query);
}

function withQuery(url: string, query: URLSearchParams): string {
  return query.size === 0 ? url : `${url}?${query.toString()}`;
}

interface EnvironmentSidesProps {
  readonly right: Scope;
  readonly others: readonly Scope[];
  readonly state: DriftState;
  readonly onChange: (state: DriftState) => void;
  readonly onSwap: () => void;
}

function EnvironmentSides({ right, others, state, onChange, onSwap }: EnvironmentSidesProps) {
  const current = others.findIndex(
    (scope) => state.left !== undefined && sameScope(scope, leftScope(right, state.left)),
  );
  return (
    <>
      <label className="select-wrap">
        <span className="sr-only">Left side</span>
        <select
          className="chip-select"
          value={current < 0 ? "" : String(current)}
          disabled={others.length === 0}
          onChange={(event) => {
            const picked = others[Number(event.target.value)];
            if (picked !== undefined) {
              onChange({
                ...state,
                left: { environment: picked.environment, cluster: picked.cluster },
                node: undefined,
              });
            }
          }}
        >
          {current < 0 && <option value="">Pick the left side</option>}
          {others.map((scope, index) => (
            <option key={`${scope.environment}/${scope.cluster}`} value={String(index)}>
              {scope.environment} · {scope.cluster}
            </option>
          ))}
        </select>
      </label>
      <button
        type="button"
        className="btn btn-sm"
        aria-label="Swap sides"
        disabled={state.left === undefined}
        onClick={onSwap}
      >
        ⇄
      </button>
      <span className="badge b-kind right-side" aria-label="Right side">
        {right.environment} · {right.cluster}
      </span>
      <TimeSelector
        at={state.at}
        prefix="Both at"
        liveLabel="Now"
        formLabel="Both sides at"
        onChange={(at) => {
          onChange({ ...state, at });
        }}
      />
    </>
  );
}

interface TimelineSidesProps {
  readonly right: Scope;
  readonly state: DriftState;
  readonly onChange: (state: DriftState) => void;
}

function TimelineSides({ right, state, onChange }: TimelineSidesProps) {
  return (
    <>
      <span className="badge b-kind right-side" aria-label="Both sides">
        {right.environment} · {right.cluster}
      </span>
      <TimeSelector
        at={state.from}
        prefix="From"
        allowLive={false}
        formLabel="From"
        onChange={(from) => {
          onChange({ ...state, from, node: undefined });
        }}
      />
      <TimeSelector
        at={state.to}
        prefix="To"
        liveLabel="To now"
        formLabel="To"
        onChange={(to) => {
          onChange({ ...state, to, node: undefined });
        }}
      />
    </>
  );
}

interface CountersProps {
  readonly diff: TopologyDiff;
  readonly sides: SideLabels;
}

function Counters({ diff, sides }: CountersProps) {
  const c = counters(diff);
  const items = [
    { kind: "added", value: c.nodesAdded, caption: `nodes only in ${sides.right}` },
    { kind: "removed", value: c.nodesRemoved, caption: `nodes only in ${sides.left}` },
    { kind: "changed", value: c.nodesChanged, caption: "changed nodes" },
    { kind: "added", value: c.edgesAdded, caption: `dependencies only in ${sides.right}` },
    { kind: "removed", value: c.edgesRemoved, caption: `dependencies only in ${sides.left}` },
  ] as const;
  return (
    <ul className="counters" aria-label="Differences">
      {items.map((item) => (
        <li className="counter" key={item.caption}>
          <span className={`glyph b-${item.kind}`} aria-hidden="true">
            {GLYPHS[item.kind]}
          </span>
          <span className="counter-text">
            <b>{item.value}</b>
            <span>{item.caption}</span>
          </span>
        </li>
      ))}
    </ul>
  );
}

interface DriftMapProps {
  readonly diff: TopologyDiff;
  readonly sides: SideLabels;
  readonly state: DriftState;
  readonly graph: UseQueryResult<TopologyGraph>;
  readonly onSelect: (node: string | undefined) => void;
}

function DriftMap({ diff, sides, state, graph, onSelect }: DriftMapProps) {
  if (graph.isPending) {
    return <output className="status">Loading the graph of {sides.right}…</output>;
  }
  if (graph.isError) {
    return (
      <p className="status status-error" role="alert">
        The control plane did not answer: {describeError(graph.error)}
      </p>
    );
  }
  const union = unionGraph(graph.data, diff);
  if (union.nodes.length === 0) {
    return (
      <p className="empty">Both sides are empty: no snapshot has reached the control plane.</p>
    );
  }
  return (
    <ServiceMap
      graph={union}
      state={{
        ...INITIAL_MAP_STATE,
        selection: state.node === undefined ? undefined : { kind: "node", id: state.node },
      }}
      overlay={overlay(diff)}
      sides={sides}
      onSelect={(selection) => {
        onSelect(selection?.kind === "node" ? selection.id : undefined);
      }}
    />
  );
}
