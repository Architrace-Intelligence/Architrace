/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { Link } from "react-router";
import type { ScopeSummary } from "../api/client";
import { type GroupBy, type ScopeGroup, scopePath } from "./filters";
import { plural } from "./format";
import { formatRelative } from "./time";

interface ScopeGroupsProps {
  readonly groups: readonly ScopeGroup[];
  readonly groupBy: GroupBy;
  readonly now: number;
}

const COLUMN_LABELS: Record<GroupBy, string> = {
  project: "Project",
  environment: "Environment",
  cluster: "Cluster",
};

export function ScopeGroups({ groups, groupBy, now }: ScopeGroupsProps) {
  const columns = (["project", "environment", "cluster"] as const).filter(
    (column) => column !== groupBy,
  );
  return (
    <>
      {groups.map((group) => (
        <section className="group" key={group.key} aria-label={group.key}>
          <div className="group-h">
            <h3 className={groupBy === "cluster" ? "mono" : undefined}>{group.key}</h3>
            <span className="group-meta">{describeGroup(group, groupBy)}</span>
          </div>
          <table className="scopes">
            <thead>
              <tr>
                {columns.map((column) => (
                  <th key={column} scope="col">
                    {COLUMN_LABELS[column]}
                  </th>
                ))}
                <th scope="col" className="num">
                  Namespaces
                </th>
                <th scope="col" className="num">
                  Services
                </th>
                <th scope="col" className="num">
                  Data streams
                </th>
                <th scope="col">Findings</th>
                <th scope="col">Agents</th>
                <th scope="col" className="num">
                  Last snapshot
                </th>
              </tr>
            </thead>
            <tbody>
              {group.scopes.map((scope) => (
                <ScopeRow key={scopePath(scope.scope)} scope={scope} columns={columns} now={now} />
              ))}
            </tbody>
          </table>
        </section>
      ))}
    </>
  );
}

interface ScopeRowProps {
  readonly scope: ScopeSummary;
  readonly columns: readonly GroupBy[];
  readonly now: number;
}

function ScopeRow({ scope, columns, now }: ScopeRowProps) {
  const path = scopePath(scope.scope);
  const quiet = scope.services === 0 && scope.dataStreams === 0;
  return (
    <tr className={quiet ? "row faint" : "row"}>
      {columns.map((column, index) => (
        <td key={column}>
          {index === 0 ? (
            <Link to={path} className="row-link">
              {cell(column, scope.scope[column])}
            </Link>
          ) : (
            cell(column, scope.scope[column])
          )}
        </td>
      ))}
      <td className="num">{scope.namespaces}</td>
      <td className="num">{scope.services}</td>
      <td className="num">{scope.dataStreams}</td>
      <td>{describeFindings(scope, path)}</td>
      <td>{describeAgents(scope)}</td>
      <td className="num">{formatRelative(scope.lastSnapshotAt, now)}</td>
    </tr>
  );
}

function cell(column: GroupBy, value: string) {
  if (column === "environment") {
    return <span className="badge b-kind">{value}</span>;
  }
  return <span className={column === "cluster" ? "mono" : undefined}>{value}</span>;
}

function describeFindings(scope: ScopeSummary, path: string) {
  const { high, medium, low } = scope.findings;
  if (high + medium + low === 0) {
    return <span className="faint">—</span>;
  }
  return (
    <Link to={`${path}/findings`} className="badges">
      {high > 0 && <span className="badge sev-high">{high} high</span>}
      {medium > 0 && <span className="badge sev-medium">{medium} medium</span>}
      {low > 0 && <span className="badge sev-low">{low} low</span>}
    </Link>
  );
}

function describeAgents(scope: ScopeSummary) {
  if (scope.agents === 0) {
    return <span className="faint">—</span>;
  }
  if (scope.liveAgents === 0) {
    return <span className="badge b-stale">stale · {scope.agents}</span>;
  }
  return (
    <span className="agents">
      <span className="dot dot-ok" aria-hidden="true" />
      {scope.liveAgents} live{scope.liveAgents < scope.agents ? ` of ${String(scope.agents)}` : ""}
    </span>
  );
}

function describeGroup(group: ScopeGroup, groupBy: GroupBy): string {
  const parts = [
    groupBy === "project" ? null : plural(group.projects, "project"),
    groupBy === "environment" ? null : plural(group.environments, "environment"),
    groupBy === "cluster" ? null : plural(group.clusters, "cluster"),
    plural(group.services, "service"),
    plural(group.dataStreams, "data stream"),
  ];
  return parts.filter((part) => part !== null).join(" · ");
}
