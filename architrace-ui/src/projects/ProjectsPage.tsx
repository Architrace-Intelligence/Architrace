/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { useSearchParams } from "react-router";
import { ProblemError } from "../api/client";
import { scopesQuery } from "../api/queries";
import { Shell } from "../app/Shell";
import { FacetChip } from "./FacetChip";
import {
  applyFilter,
  facetValues,
  GROUP_BY_OPTIONS,
  type GroupBy,
  groupScopes,
  isEmptyFilter,
  parseFilter,
  type ProjectsFilter,
  summarise,
  type Summary,
  toggleFacetValue,
  toParams,
  EMPTY_FILTER,
} from "./filters";
import { plural } from "./format";
import { ScopeGroups } from "./ScopeGroups";

const GROUP_LABELS: Record<GroupBy, string> = {
  project: "Project",
  environment: "Environment",
  cluster: "Cluster",
};

export function ProjectsPage() {
  const [params, setParams] = useSearchParams();
  const filter = parseFilter(params);
  const scopes = useQuery(scopesQuery());
  const update = (next: ProjectsFilter) => {
    setParams(toParams(next), { replace: true });
  };

  const search = (
    <input
      className="ask-input"
      type="search"
      aria-label="Filter scopes"
      placeholder="Filter by project, environment or cluster…"
      value={filter.query}
      onChange={(event) => {
        update({ ...filter, query: event.target.value });
      }}
    />
  );

  return (
    <Shell title="Projects" tools={search} apiRequest="/api/v1/scopes">
      {scopes.isPending && <output className="status">Connecting to the control plane…</output>}
      {scopes.isError && (
        <p className="status status-error" role="alert">
          The control plane did not answer: {describe(scopes.error)}
        </p>
      )}
      {scopes.isSuccess && <ScopeList all={scopes.data} filter={filter} onChange={update} />}
    </Shell>
  );
}

interface ScopeListProps {
  readonly all: readonly ProjectsFilterSource[];
  readonly filter: ProjectsFilter;
  readonly onChange: (filter: ProjectsFilter) => void;
}

type ProjectsFilterSource = Parameters<typeof applyFilter>[0][number];

function ScopeList({ all, filter, onChange }: ScopeListProps) {
  const visible = applyFilter(all, filter);
  const groups = groupScopes(visible, filter.groupBy);
  const summary = summarise(all, visible);
  const [now] = useState(() => Date.now());
  return (
    <>
      <div className="toolbar">
        <FacetChip
          label="Environment"
          values={facetValues(all, filter, "environment")}
          onToggle={(value) => {
            onChange(toggleFacetValue(filter, "environment", value));
          }}
        />
        <FacetChip
          label="Cluster"
          values={facetValues(all, filter, "cluster")}
          onToggle={(value) => {
            onChange(toggleFacetValue(filter, "cluster", value));
          }}
        />
        {!isEmptyFilter(filter) && (
          <button
            type="button"
            className="btn btn-ghost btn-sm"
            onClick={() => {
              onChange({ ...EMPTY_FILTER, groupBy: filter.groupBy });
            }}
          >
            Clear all
          </button>
        )}
        <div className="toolbar-right">
          <div className="seg" role="group" aria-label="Group by">
            <span className="seg-label">Group by</span>
            {GROUP_BY_OPTIONS.map((option) => (
              <button
                key={option}
                type="button"
                aria-pressed={filter.groupBy === option}
                onClick={() => {
                  onChange({ ...filter, groupBy: option });
                }}
              >
                {GROUP_LABELS[option]}
              </button>
            ))}
          </div>
        </div>
      </div>
      <output className="count-line">{describeSummary(summary)}</output>
      {all.length === 0 && (
        <p className="empty">
          No agent has registered yet. Start an agent and this list fills itself.
        </p>
      )}
      {all.length > 0 && visible.length === 0 && (
        <p className="empty">No scope matches the filter.</p>
      )}
      <ScopeGroups groups={groups} groupBy={filter.groupBy} now={now} />
    </>
  );
}

function describeSummary(summary: Summary): string {
  const scopes =
    summary.visible === summary.total
      ? plural(summary.total, "scope")
      : `${String(summary.visible)} of ${plural(summary.total, "scope")}`;
  const agents =
    summary.staleAgents > 0
      ? `${plural(summary.agents, "agent")}, ${String(summary.staleAgents)} stale`
      : plural(summary.agents, "agent");
  return [
    scopes,
    plural(summary.projects, "project"),
    plural(summary.environments, "environment"),
    plural(summary.clusters, "cluster"),
    agents,
  ].join(" · ");
}

function describe(error: Error): string {
  return error instanceof ProblemError ? (error.problem.detail ?? error.message) : error.message;
}
