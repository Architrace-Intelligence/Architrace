/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import type { Finding } from "../api/client";
import { plural } from "../projects/format";
import { countBySeverity, RULES, ruleInfo, type RuleInfo, severityToken } from "./model";

interface FindingsPanelProps {
  readonly findings: readonly Finding[];
  readonly environment: string;
}

export function FindingsPanel({ findings, environment }: FindingsPanelProps) {
  const counts = countBySeverity(findings);
  const extra = findings
    .map((finding) => finding.ruleId)
    .filter(
      (ruleId, index, ids) =>
        ids.indexOf(ruleId) === index && RULES.every((rule) => rule.id !== ruleId),
    )
    .map((ruleId) => ruleInfo(ruleId, findings));
  return (
    <>
      <div className="panel-head">
        <span className="panel-kicker">Rules · {environment}</span>
        <div className="stat-row" aria-label="Findings by severity">
          <Stat label="high" value={counts.high} />
          <Stat label="medium" value={counts.medium} />
          <Stat label="low" value={counts.low} />
        </div>
      </div>
      <div className="panel-body">
        <span className="panel-kicker">Every rule · deterministic</span>
        {[...RULES, ...extra].map((rule) => (
          <RuleCard key={rule.id} rule={rule} findings={findings} />
        ))}
        <p className="note">
          Thresholds are the defaults; the property behind each rule changes it on the control
          plane.
        </p>
      </div>
    </>
  );
}

interface StatProps {
  readonly label: string;
  readonly value: number;
}

function Stat({ label, value }: StatProps) {
  return (
    <span className={`stat sev-${label}`}>
      <b>{value}</b>
      <span>{label}</span>
    </span>
  );
}

interface RuleCardProps {
  readonly rule: RuleInfo;
  readonly findings: readonly Finding[];
}

function RuleCard({ rule, findings }: RuleCardProps) {
  const count = findings.filter((finding) => finding.ruleId === rule.id).length;
  return (
    <article className="sentence">
      <span className="panel-row">
        <span className={count === 0 ? "badge b-ok" : `badge sev-${severityToken(rule.severity)}`}>
          {count === 0 ? "pass" : plural(count, "finding")}
        </span>
        <b>{rule.label}</b>
      </span>
      <span className="muted">{rule.threshold}</span>
      {rule.property !== undefined && <span className="mono faint">{rule.property}</span>}
    </article>
  );
}
