/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { useQuery } from "@tanstack/react-query";
import { Link, useNavigate, useParams, useSearchParams } from "react-router";
import { describeError, type Finding, type Severity } from "../api/client";
import { findingsQuery, graphQuery, scopesQuery } from "../api/queries";
import { Shell } from "../app/Shell";
import { formatInstant } from "../map/metrics";
import { scopePath } from "../projects/filters";
import { plural } from "../projects/format";
import { ScopeSwitcher } from "../scope/ScopeSwitcher";
import { FindingsPanel } from "./FindingsPanel";
import {
  allowlistLine,
  apiRequest,
  applyFilter,
  countBySeverity,
  evaluatedAt,
  type FindingGroup,
  findingKey,
  type FindingsFilter,
  groupByRule,
  mapSearch,
  nameOf,
  nodeNames,
  parseFindingsFilter,
  passingRules,
  RULES,
  SEVERITIES,
  SEVERITY_LABELS,
  severityToken,
  toFindingsParams,
} from "./model";

export function FindingsPage() {
  const { project = "", environment = "", cluster = "" } = useParams();
  const scope = { project, environment, cluster };
  const path = scopePath(scope);
  const [params, setParams] = useSearchParams();
  const navigate = useNavigate();
  const filter = parseFindingsFilter(params);
  const findings = useQuery(findingsQuery(scope));
  const graph = useQuery(graphQuery(scope));
  const scopes = useQuery(scopesQuery());
  const update = (next: FindingsFilter) => {
    setParams(toFindingsParams(next), { replace: true });
  };
  const all = findings.data ?? [];
  const shown = applyFilter(all, filter);
  const counts = countBySeverity(all);
  const names = nodeNames(graph.data);
  const tools = (
    <ScopeSwitcher
      current={scope}
      scopes={scopes.data?.map((summary) => summary.scope) ?? []}
      onSwitch={(target) => {
        void navigate({ pathname: `${scopePath(target)}/findings`, search: params.toString() });
      }}
    />
  );
  const aside = findings.isSuccess ? (
    <FindingsPanel findings={all} environment={environment} />
  ) : undefined;
  return (
    <Shell
      title="Findings"
      tools={tools}
      apiRequest={apiRequest(path, filter)}
      scopePath={path}
      aside={aside}
    >
      <div className="toolbar">
        <fieldset className="chips">
          <legend className="sr-only">Severity</legend>
          <button
            type="button"
            className={filter.severity === undefined ? "chip chip-on" : "chip"}
            aria-pressed={filter.severity === undefined}
            onClick={() => {
              update({ ...filter, severity: undefined });
            }}
          >
            All <span className="faint mono chip-count">{all.length}</span>
          </button>
          {SEVERITIES.map((severity) => (
            <SeverityChip
              key={severity}
              severity={severity}
              count={counts[SEVERITY_LABELS[severity]]}
              pressed={filter.severity === severity}
              onPress={() => {
                update({
                  ...filter,
                  severity: filter.severity === severity ? undefined : severity,
                });
              }}
            />
          ))}
        </fieldset>
        <label className="select-wrap">
          <span className="sr-only">Rule</span>
          <select
            className="chip-select"
            value={filter.rule ?? ""}
            onChange={(event) => {
              update({
                ...filter,
                rule: event.target.value === "" ? undefined : event.target.value,
              });
            }}
          >
            <option value="">All rules</option>
            {RULES.map((rule) => (
              <option key={rule.id} value={rule.id}>
                {rule.label}
              </option>
            ))}
          </select>
        </label>
        {findings.isSuccess && (
          <span className="count-line toolbar-right">{describeShown(all, shown)}</span>
        )}
      </div>
      {findings.isPending && <output className="status">Loading the findings…</output>}
      {findings.isError && (
        <p className="status status-error" role="alert">
          The control plane did not answer: {describeError(findings.error)}
        </p>
      )}
      {findings.isSuccess && all.length === 0 && (
        <p className="empty">
          <b>No findings.</b> The rules run after every snapshot of {environment}; a clean graph and
          a scope that has not reported yet both look like this.
        </p>
      )}
      {findings.isSuccess && all.length > 0 && shown.length === 0 && (
        <p className="empty">No finding matches the filter.</p>
      )}
      {groupByRule(shown).map((group) => (
        <Group key={group.rule.id} group={group} names={names} mapPath={path} />
      ))}
      {findings.isSuccess && passingRules(all).length > 0 && (
        <section className="stack" aria-label="Rules that pass">
          <span className="panel-kicker">Rules that pass in {environment}</span>
          <div className="chips">
            {passingRules(all).map((rule) => (
              <span key={rule.id} className="badge b-ok">
                {rule.label}
              </span>
            ))}
          </div>
        </section>
      )}
    </Shell>
  );
}

function describeShown(all: readonly Finding[], shown: readonly Finding[]): string {
  const at = evaluatedAt(all);
  const evaluated = at === undefined ? "" : `Evaluated ${formatInstant(at)} · `;
  return `${evaluated}${String(shown.length)} of ${plural(all.length, "finding")} shown`;
}

interface SeverityChipProps {
  readonly severity: Severity;
  readonly count: number;
  readonly pressed: boolean;
  readonly onPress: () => void;
}

function SeverityChip({ severity, count, pressed, onPress }: SeverityChipProps) {
  return (
    <button
      type="button"
      className={pressed ? "chip chip-on" : "chip"}
      aria-pressed={pressed}
      onClick={onPress}
    >
      <span className={`badge sev-${severityToken(severity)}`}>{SEVERITY_LABELS[severity]}</span>
      <span className="faint mono chip-count">{count}</span>
    </button>
  );
}

interface GroupProps {
  readonly group: FindingGroup;
  readonly names: ReadonlyMap<string, string>;
  readonly mapPath: string;
}

function Group({ group, names, mapPath }: GroupProps) {
  const { rule } = group;
  return (
    <details className="group" open aria-label={rule.label}>
      <summary className="group-h">
        <h3>{rule.label}</h3>
        <span className={`badge sev-${severityToken(rule.severity)}`}>{group.findings.length}</span>
        <span className="group-meta">
          {rule.threshold}
          {rule.property !== undefined && ` · ${rule.property}`}
        </span>
      </summary>
      {group.findings.map((finding) => (
        <FindingRow key={findingKey(finding)} finding={finding} names={names} mapPath={mapPath} />
      ))}
    </details>
  );
}

interface FindingRowProps {
  readonly finding: Finding;
  readonly names: ReadonlyMap<string, string>;
  readonly mapPath: string;
}

function FindingRow({ finding, names, mapPath }: FindingRowProps) {
  const line = allowlistLine(finding);
  return (
    <details className="finding">
      <summary>
        <span className={`badge sev-${severityToken(finding.severity)}`}>
          {SEVERITY_LABELS[finding.severity]}
        </span>
        <span className="diff-text">
          <b>{finding.title}</b>
          <span className="finding-subjects">
            {finding.subjectNodeIds.map((id) => (
              <span key={id} className="badge b-kind" title={id}>
                {nameOf(id, names)}
              </span>
            ))}
          </span>
        </span>
        <span className="caret" aria-hidden="true" />
      </summary>
      <div className="finding-body">
        <p className="muted">{finding.detail}</p>
        {finding.evidence.length > 0 && (
          <>
            <span className="panel-kicker">Evidence</span>
            <ol className="evidence">
              {finding.evidence.map((id, index) => (
                <li key={`${String(index)}:${id}`}>
                  {nameOf(id, names)} <span className="mono faint">{id}</span>
                </li>
              ))}
            </ol>
          </>
        )}
        <div className="finding-actions">
          <Link className="btn btn-sm" to={{ pathname: mapPath, search: mapSearch(finding) }}>
            Show on map
          </Link>
          {line !== undefined && <code className="mono">{line}</code>}
        </div>
      </div>
    </details>
  );
}
