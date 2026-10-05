/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import type { Finding, FindingCounts, Severity, TopologyGraph } from "../api/client";

export const SEVERITIES: readonly Severity[] = ["HIGH", "MEDIUM", "LOW"];

export type SeverityToken = "high" | "medium" | "low";

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

export const RULES: readonly RuleInfo[] = [
  {
    id: "cyclic-dependency",
    label: "Cyclic dependency",
    severity: "HIGH",
    threshold: "any cycle of synchronous calls",
  },
  {
    id: "shared-database",
    label: "Shared database",
    severity: "HIGH",
    threshold: "2 or more services on one database",
    property: "architrace.rules.shared-database.min-services",
  },
  {
    id: "wide-blast-radius",
    label: "Wide blast radius",
    severity: "HIGH",
    threshold: "more than 50 % of the services impaired, at least 3",
    property: "architrace.rules.wide-blast-radius.min-share-percent",
  },
  {
    id: "cross-domain-coupling",
    label: "Cross-domain coupling",
    severity: "MEDIUM",
    threshold: "more than 3 other domains called synchronously",
    property: "architrace.rules.cross-domain-coupling.max-domains",
  },
  {
    id: "fan-in-hub",
    label: "Fan-in hub",
    severity: "MEDIUM",
    threshold: "more than 8 direct callers",
    property: "architrace.rules.fan-in-hub.max-callers",
  },
  {
    id: "long-sync-chain",
    label: "Long synchronous chain",
    severity: "MEDIUM",
    threshold: "more than 5 hops",
    property: "architrace.rules.long-sync-chain.max-hops",
  },
  {
    id: "unknown-external",
    label: "Unknown external",
    severity: "LOW",
    threshold: "any external host outside the allowlist",
    property: "architrace.rules.unknown-external.allowlist",
  },
];

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
  return `/api/v1${scopePath}/findings${query === "" ? "" : `?${query}`}`;
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
