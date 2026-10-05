/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import type { Finding, FindingCounts, Severity, TopologyGraph } from "../api/client";
import type { NodeFindings, SeverityToken } from "../map/model";

export const SEVERITIES: readonly Severity[] = ["HIGH", "MEDIUM", "LOW"];

export const SEVERITY_LABELS: Record<Severity, SeverityToken> = {
  HIGH: "high",
  MEDIUM: "medium",
  LOW: "low",
};

export interface RuleInfo {
  readonly id: string;
  readonly label: string;
  readonly severity: Severity;
  readonly threshold: string;
  readonly property?: string;
}

const RULE_TABLE: readonly (readonly [string, string, Severity, string, string?])[] = [
  ["cyclic-dependency", "Cyclic dependency", "HIGH", "any cycle of synchronous calls"],
  [
    "shared-database",
    "Shared database",
    "HIGH",
    "2 or more services on one database",
    "min-services",
  ],
  [
    "wide-blast-radius",
    "Wide blast radius",
    "HIGH",
    "more than 50 % of the services impaired, at least 3",
    "min-share-percent",
  ],
  [
    "cross-domain-coupling",
    "Cross-domain coupling",
    "MEDIUM",
    "more than 3 other domains called synchronously",
    "max-domains",
  ],
  ["fan-in-hub", "Fan-in hub", "MEDIUM", "more than 8 direct callers", "max-callers"],
  ["long-sync-chain", "Long synchronous chain", "MEDIUM", "more than 5 hops", "max-hops"],
  [
    "unknown-external",
    "Unknown external",
    "LOW",
    "any external host outside the allowlist",
    "allowlist",
  ],
];

export const RULES: readonly RuleInfo[] = RULE_TABLE.map(
  ([id, label, severity, threshold, key]) => ({
    id,
    label,
    severity,
    threshold,
    property: key === undefined ? undefined : `architrace.rules.${id}.${key}`,
  }),
);

export interface FindingsFilter {
  readonly severity?: Severity;
  readonly rule?: string;
}

export function parseFindingsFilter(params: URLSearchParams): FindingsFilter {
  const severity = params.get("severity");
  const rule = params.get("rule");
  return {
    severity: SEVERITIES.find((candidate) => candidate === severity),
    rule: rule === null || rule === "" ? undefined : rule,
  };
}

export function toFindingsParams(filter: FindingsFilter): URLSearchParams {
  const params = new URLSearchParams();
  if (filter.severity !== undefined) {
    params.set("severity", filter.severity);
  }
  if (filter.rule !== undefined) {
    params.set("rule", filter.rule);
  }
  return params;
}

export function apiRequest(scopePath: string, filter: FindingsFilter): string {
  const query = toFindingsParams(filter).toString();
  const suffix = query === "" ? "" : `?${query}`;
  return `/api/v1${scopePath}/findings${suffix}`;
}

export function severityToken(severity: Severity): SeverityToken {
  return SEVERITY_LABELS[severity];
}

export function countBySeverity(findings: readonly Finding[]): FindingCounts {
  return {
    high: findings.filter((finding) => finding.severity === "HIGH").length,
    medium: findings.filter((finding) => finding.severity === "MEDIUM").length,
    low: findings.filter((finding) => finding.severity === "LOW").length,
  };
}

export function applyFilter(findings: readonly Finding[], filter: FindingsFilter): Finding[] {
  return findings.filter(
    (finding) =>
      (filter.severity === undefined || finding.severity === filter.severity) &&
      (filter.rule === undefined || finding.ruleId === filter.rule),
  );
}

export interface FindingGroup {
  readonly rule: RuleInfo;
  readonly findings: readonly Finding[];
}

export function ruleInfo(ruleId: string, findings: readonly Finding[]): RuleInfo {
  return (
    RULES.find((rule) => rule.id === ruleId) ?? {
      id: ruleId,
      label: ruleId,
      severity: findings.find((finding) => finding.ruleId === ruleId)?.severity ?? "LOW",
      threshold: "",
    }
  );
}

export function groupByRule(findings: readonly Finding[]): FindingGroup[] {
  const known = RULES.map((rule) => rule.id);
  const unknown = findings
    .map((finding) => finding.ruleId)
    .filter((ruleId, index, ids) => !known.includes(ruleId) && ids.indexOf(ruleId) === index);
  return [...known, ...unknown]
    .map((ruleId) => ({
      rule: ruleInfo(ruleId, findings),
      findings: findings.filter((finding) => finding.ruleId === ruleId),
    }))
    .filter((group) => group.findings.length > 0);
}

export function passingRules(findings: readonly Finding[]): RuleInfo[] {
  return RULES.filter((rule) => findings.every((finding) => finding.ruleId !== rule.id));
}

export function evaluatedAt(findings: readonly Finding[]): string | undefined {
  return findings
    .map((finding) => finding.evaluatedAt)
    .reduce<string | undefined>(
      (latest, candidate) => (latest === undefined || candidate > latest ? candidate : latest),
      undefined,
    );
}

export function findingKey(finding: Finding): string {
  return `${finding.ruleId}:${finding.subjectNodeIds.join(",")}`;
}

export function nodeNames(graph: TopologyGraph | undefined): ReadonlyMap<string, string> {
  return new Map((graph?.nodes ?? []).map((node) => [node.id, node.name]));
}

export function nameOf(id: string, names: ReadonlyMap<string, string>): string {
  return names.get(id) ?? id.slice(id.indexOf(":") + 1);
}

export function allowlistLine(finding: Finding): string | undefined {
  if (finding.ruleId !== "unknown-external") {
    return undefined;
  }
  const host = finding.subjectNodeIds.map((id) => id.slice(id.indexOf(":") + 1)).join(",");
  return `architrace.rules.unknown-external.allowlist: ${host}`;
}

export function mapSearch(finding: Finding): string {
  const subject = finding.subjectNodeIds[0];
  return subject === undefined ? "" : `?node=${encodeURIComponent(subject)}`;
}

const SEVERITY_RANK: Record<SeverityToken, number> = { high: 0, medium: 1, low: 2 };

export function findingsByNode(findings: readonly Finding[]): ReadonlyMap<string, NodeFindings> {
  const byNode = new Map<string, NodeFindings>();
  findings.forEach((finding) => {
    const severity = severityToken(finding.severity);
    finding.subjectNodeIds.forEach((id) => {
      const current = byNode.get(id);
      byNode.set(id, {
        count: (current?.count ?? 0) + 1,
        severity:
          current === undefined || SEVERITY_RANK[severity] < SEVERITY_RANK[current.severity]
            ? severity
            : current.severity,
      });
    });
  });
  return byNode;
}

export function findingsOf(findings: readonly Finding[], nodeId: string): Finding[] {
  return findings.filter((finding) => finding.subjectNodeIds.includes(nodeId));
}
