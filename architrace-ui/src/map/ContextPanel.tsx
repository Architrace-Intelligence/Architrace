/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { Fragment, type ReactNode } from "react";
import type { Finding, Impact, ImpactedNode, TopologyGraph, TopologyNode } from "../api/client";
import { findingsOf, SEVERITY_LABELS, severityToken } from "../findings/model";
import { plural } from "../projects/format";
import { describeMetrics, formatCount, formatInstant, formatMillis, formatRate } from "./metrics";
import {
  clustersOf,
  countByType,
  type Dependency,
  type EdgeDetails,
  type EdgeHealth,
  edgeDetails,
  edgeHealth,
  edgeId,
  type MapState,
  namespacesOf,
  NODE_TYPE_LABELS,
  type NodeDetails,
  nodeDetails,
  type Selection,
  type StreamSummary,
  streams,
} from "./model";
import { NodeIcon } from "./NodeIcon";

interface ContextPanelProps {
  readonly graph: TopologyGraph;
  readonly state: MapState;
  readonly onSelect: (selection: Selection | undefined) => void;
  readonly findings?: readonly Finding[];
  readonly impact?: Impact;
  readonly onImpact?: () => void;
}

const HEALTH_LABELS: Record<EdgeHealth, string> = {
  ok: "healthy",
  warn: "errors ≥ 1 %",
  bad: "errors ≥ 3 %",
};

export function ContextPanel({
  graph,
  state,
  onSelect,
  findings,
  impact,
  onImpact,
}: ContextPanelProps) {
  const environment = graph.scope.environment;
  const node =
    state.selection?.kind === "node" ? nodeDetails(graph, state.selection.id) : undefined;
  if (node !== undefined) {
    return (
      <NodePanel
        details={node}
        environment={environment}
        onSelect={onSelect}
        findings={findings === undefined ? undefined : findingsOf(findings, node.node.id)}
        impact={impact?.subject.id === node.node.id ? impact : undefined}
        onImpact={state.lens === "impact" ? undefined : onImpact}
        names={new Map(graph.nodes.map((candidate) => [candidate.id, candidate.name]))}
      />
    );
  }
  const edge =
    state.selection?.kind === "edge" ? edgeDetails(graph, state.selection.id) : undefined;
  if (edge !== undefined) {
    return <EdgePanel details={edge} environment={environment} onSelect={onSelect} />;
  }
  return <ScopePanel graph={graph} onSelect={onSelect} />;
}

interface ScopePanelProps {
  readonly graph: TopologyGraph;
  readonly onSelect: (selection: Selection | undefined) => void;
}

function ScopePanel({ graph, onSelect }: ScopePanelProps) {
  const counts = countByType(graph.nodes);
  const streamEdges = graph.edges.filter((edge) => edge.kind !== "SYNC").length;
  const namespaces = new Set(graph.nodes.flatMap(namespacesOf)).size;
  const summaries = streams(graph);
  return (
    <>
      <div className="panel-head">
        <span className="panel-kicker">Scope · {graph.scope.environment}</span>
        <div className="panel-title">
          <b>{graph.scope.project}</b>
          <span className="mono faint">{graph.scope.cluster}</span>
        </div>
      </div>
      <div className="panel-body">
        <dl className="kv">
          <dt>Services</dt>
          <dd>
            {counts.SERVICE} <span className="faint">in {plural(namespaces, "namespace")}</span>
          </dd>
          <dt>Data stores</dt>
          <dd>{counts.DATABASE}</dd>
          <dt>Data streams</dt>
          <dd>{counts.TOPIC}</dd>
          <dt>External hosts</dt>
          <dd>{counts.EXTERNAL}</dd>
          <dt>Dependencies</dt>
          <dd>
            {graph.edges.length}{" "}
            <span className="faint">
              · {graph.edges.length - streamEdges} sync, {streamEdges} stream
            </span>
          </dd>
          <dt>Graph at</dt>
          <dd className="mono">{formatInstant(graph.at)}</dd>
        </dl>
        <Section title="Data streams">
          {summaries.length === 0 && <span className="faint">No data stream in this graph.</span>}
          {summaries.map((stream) => (
            <button
              key={stream.topic.id}
              type="button"
              className="stream"
              onClick={() => {
                onSelect({ kind: "node", id: stream.topic.id });
              }}
            >
              <NodeIcon type="TOPIC" />{" "}
              <span className="stream-text">
                <b>{stream.topic.name}</b> <span>{describeFlow(stream)}</span>
              </span>{" "}
              <span className="dep-metrics">{formatCount(stream.messages)}</span>
            </button>
          ))}
        </Section>
      </div>
    </>
  );
}

function describeFlow(stream: StreamSummary): string {
  const names = (nodes: readonly TopologyNode[]) =>
    nodes.length === 0 ? "nobody" : nodes.map((node) => node.name).join(", ");
  return `${names(stream.producers)} → ${names(stream.consumers)}`;
}

interface NodePanelProps {
  readonly details: NodeDetails;
  readonly environment: string;
  readonly onSelect: (selection: Selection | undefined) => void;
  readonly findings?: readonly Finding[];
  readonly impact?: Impact;
  readonly onImpact?: () => void;
  readonly names: ReadonlyMap<string, string>;
}

function NodePanel({
  details,
  environment,
  onSelect,
  findings,
  impact,
  onImpact,
  names,
}: NodePanelProps) {
  const { node, inbound, outbound } = details;
  const namespaces = namespacesOf(node);
  const clusters = clustersOf(node);
  const syncIn = inbound.filter((dependency) => dependency.edge.kind === "SYNC");
  const syncOut = outbound.filter((dependency) => dependency.edge.kind === "SYNC");
  const published = outbound.filter((dependency) => dependency.edge.kind === "PUBLISH");
  const consumed = inbound.filter((dependency) => dependency.edge.kind === "CONSUME");
  const producers = inbound.filter((dependency) => dependency.edge.kind === "PUBLISH");
  const consumers = outbound.filter((dependency) => dependency.edge.kind === "CONSUME");
  return (
    <>
      <div className="panel-head">
        <div className="panel-row">
          <span className="panel-kicker">
            {NODE_TYPE_LABELS[node.type].singular} · {environment}
          </span>
          <CloseButton onSelect={onSelect} />
        </div>
        <div className="panel-row">
          <NodeIcon type={node.type} size="large" />
          <div className="panel-title">
            <b>{node.name}</b>
            <span className="mono faint">{node.id}</span>
          </div>
        </div>
        {(namespaces.length > 0 || node.versions.length > 0) && (
          <div className="badges">
            {namespaces.map((namespace) => (
              <span key={namespace} className="badge b-kind">
                namespace {namespace}
              </span>
            ))}
            {node.versions.map((version) => (
              <span key={version} className="badge b-kind">
                v{version}
              </span>
            ))}
          </div>
        )}
      </div>
      <div className="panel-body">
        <dl className="kv">
          {clusters.length > 0 && (
            <>
              <dt>Clusters</dt>
              <dd className="mono">{clusters.join(", ")}</dd>
            </>
          )}
          {Object.entries(node.labels).map(([key, value]) => (
            <Fragment key={key}>
              <dt>{key}</dt>
              <dd className="mono">{value}</dd>
            </Fragment>
          ))}
          <dt>Inbound</dt>
          <dd>{describeDependencies(inbound)}</dd>
          <dt>Outbound</dt>
          <dd>{describeDependencies(outbound)}</dd>
        </dl>
        {node.type === "SERVICE" && (
          <Section title="Data streams">
            {published.length === 0 && consumed.length === 0 && (
              <span className="faint">Neither publishes nor consumes a data stream.</span>
            )}
            {published.map((dependency) => (
              <DependencyRow
                key={edgeId(dependency.edge)}
                badge="publish"
                dependency={dependency}
                onSelect={onSelect}
              />
            ))}
            {consumed.map((dependency) => (
              <DependencyRow
                key={edgeId(dependency.edge)}
                badge="consume"
                dependency={dependency}
                onSelect={onSelect}
              />
            ))}
          </Section>
        )}
        {node.type === "TOPIC" && (
          <>
            <Section title={`Producers · ${String(producers.length)}`}>
              {producers.length === 0 && <span className="faint">No producer in this window.</span>}
              {producers.map((dependency) => (
                <DependencyRow
                  key={edgeId(dependency.edge)}
                  badge="publish"
                  dependency={dependency}
                  onSelect={onSelect}
                />
              ))}
            </Section>
            <Section title={`Consumers · ${String(consumers.length)}`}>
              {consumers.length === 0 && <span className="faint">No consumer in this window.</span>}
              {consumers.map((dependency) => (
                <DependencyRow
                  key={edgeId(dependency.edge)}
                  badge="consume"
                  dependency={dependency}
                  onSelect={onSelect}
                />
              ))}
            </Section>
          </>
        )}
        {impact !== undefined && <ImpactCard impact={impact} names={names} onSelect={onSelect} />}
        {impact === undefined && onImpact !== undefined && (
          <button type="button" className="btn btn-sm" onClick={onImpact}>
            What breaks if it fails
          </button>
        )}
        {findings !== undefined && (
          <Section title={`Findings · ${String(findings.length)}`}>
            {findings.length === 0 && <span className="faint">No finding on this node.</span>}
            {findings.map((finding) => (
              <span className="panel-row" key={`${finding.ruleId}:${finding.title}`}>
                <span className={`badge sev-${severityToken(finding.severity)}`}>
                  {SEVERITY_LABELS[finding.severity]}
                </span>
                <span>{finding.title}</span>
              </span>
            ))}
          </Section>
        )}
        {node.type !== "TOPIC" && (
          <Section
            title={`Dependencies · ${String(syncIn.length)} in, ${String(syncOut.length)} out`}
          >
            {syncIn.length === 0 && syncOut.length === 0 && (
              <span className="faint">No synchronous dependency in this window.</span>
            )}
            {syncIn.map((dependency) => (
              <DependencyRow
                key={edgeId(dependency.edge)}
                badge="in"
                dependency={dependency}
                onSelect={onSelect}
              />
            ))}
            {syncOut.map((dependency) => (
              <DependencyRow
                key={edgeId(dependency.edge)}
                badge="out"
                dependency={dependency}
                onSelect={onSelect}
              />
            ))}
          </Section>
        )}
      </div>
    </>
  );
}

function describeDependencies(dependencies: readonly Dependency[]): string {
  const calls = dependencies.reduce(
    (total, dependency) => total + dependency.edge.metrics.calls,
    0,
  );
  return `${plural(dependencies.length, "dependency", "dependencies")} · ${formatCount(calls)} calls`;
}

interface DependencyRowProps {
  readonly badge: string;
  readonly dependency: Dependency;
  readonly onSelect: (selection: Selection | undefined) => void;
}

function DependencyRow({ badge, dependency, onSelect }: DependencyRowProps) {
  return (
    <button
      type="button"
      className="dep"
      onClick={() => {
        onSelect({ kind: "edge", id: edgeId(dependency.edge) });
      }}
    >
      <span className="badge b-kind">{badge}</span>{" "}
      <span className="dep-name">{dependency.other.name}</span>{" "}
      <span className="dep-metrics">{describeMetrics(dependency.edge.metrics)}</span>
    </button>
  );
}

interface EdgePanelProps {
  readonly details: EdgeDetails;
  readonly environment: string;
  readonly onSelect: (selection: Selection | undefined) => void;
}

function EdgePanel({ details, environment, onSelect }: EdgePanelProps) {
  const { edge, source, target } = details;
  const health = edgeHealth(edge.metrics);
  const { metrics } = edge;
  return (
    <>
      <div className="panel-head">
        <div className="panel-row">
          <span className="panel-kicker">Dependency · {environment}</span>
          <CloseButton onSelect={onSelect} />
        </div>
        <div className="panel-title">
          <b>
            {source.name} → {target.name}
          </b>
          <span className="mono faint">{edgeId(edge)}</span>
        </div>
        <div className="badges">
          <span className="badge b-kind">{edge.kind.toLowerCase()}</span>
          <span className={`badge b-${health}`}>{HEALTH_LABELS[health]}</span>
        </div>
      </div>
      <div className="panel-body">
        <dl className="kv">
          <dt>Calls</dt>
          <dd className="mono">{metrics.calls}</dd>
          <dt>Errors</dt>
          <dd className="mono">
            {metrics.errors} <span className="faint">· {formatRate(metrics)}</span>
          </dd>
          <dt>p50 / p95</dt>
          <dd className="mono">
            {formatMillis(metrics.p50Millis)} / {formatMillis(metrics.p95Millis)}
          </dd>
          <dt>p99 / max</dt>
          <dd className="mono">
            {formatMillis(metrics.p99Millis)} / {formatMillis(metrics.maxMillis)}
          </dd>
        </dl>
        <Section title="Ends">
          <NodeRow badge="from" node={source} onSelect={onSelect} />
          <NodeRow badge="to" node={target} onSelect={onSelect} />
        </Section>
      </div>
    </>
  );
}

interface ImpactCardProps {
  readonly impact: Impact;
  readonly names: ReadonlyMap<string, string>;
  readonly onSelect: (selection: Selection | undefined) => void;
}

function ImpactCard({ impact, names, onSelect }: ImpactCardProps) {
  const share =
    impact.servicesTotal === 0 ? 0 : Math.round((impact.services * 100) / impact.servicesTotal);
  return (
    <Section title={`If ${impact.subject.name} fails · deterministic`}>
      <span className="muted">
        {impact.impaired.length === 0
          ? "Nothing depends on it synchronously."
          : `${String(impact.services)} of ${plural(impact.servicesTotal, "service")} impaired (${String(share)} %)`}
        {impact.delayed.length > 0 && ` · ${plural(impact.delayed.length, "node")} delayed`}
      </span>
      {impact.impaired.map((reached) => (
        <ImpactRow
          key={reached.node.id}
          badge={`${String(reached.distance)} hop${reached.distance === 1 ? "" : "s"}`}
          badgeClass="sev-high"
          reached={reached}
          names={names}
          onSelect={onSelect}
        />
      ))}
      {impact.delayed.map((reached) => (
        <ImpactRow
          key={reached.node.id}
          badge="delayed"
          badgeClass="sev-medium"
          reached={reached}
          names={names}
          onSelect={onSelect}
        />
      ))}
    </Section>
  );
}

interface ImpactRowProps {
  readonly badge: string;
  readonly badgeClass: string;
  readonly reached: ImpactedNode;
  readonly names: ReadonlyMap<string, string>;
  readonly onSelect: (selection: Selection | undefined) => void;
}

function ImpactRow({ badge, badgeClass, reached, names, onSelect }: ImpactRowProps) {
  return (
    <button
      type="button"
      className="dep"
      onClick={() => {
        onSelect({ kind: "node", id: reached.node.id });
      }}
    >
      <span className={`badge ${badgeClass}`}>{badge}</span>{" "}
      <span className="dep-name">{reached.node.name}</span>{" "}
      <span className="dep-metrics">
        {reached.path.map((id) => names.get(id) ?? id).join(" → ")}
      </span>
    </button>
  );
}

interface NodeRowProps {
  readonly badge: string;
  readonly node: TopologyNode;
  readonly onSelect: (selection: Selection | undefined) => void;
}

function NodeRow({ badge, node, onSelect }: NodeRowProps) {
  return (
    <button
      type="button"
      className="dep"
      onClick={() => {
        onSelect({ kind: "node", id: node.id });
      }}
    >
      <span className="badge b-kind">{badge}</span> <span className="dep-name">{node.name}</span>{" "}
      <span className="dep-metrics">{NODE_TYPE_LABELS[node.type].singular}</span>
    </button>
  );
}

interface SectionProps {
  readonly title: string;
  readonly children: ReactNode;
}

function Section({ title, children }: SectionProps) {
  return (
    <section className="stack" aria-label={title}>
      <span className="panel-kicker">{title}</span>
      {children}
    </section>
  );
}

interface CloseButtonProps {
  readonly onSelect: (selection: Selection | undefined) => void;
}

function CloseButton({ onSelect }: CloseButtonProps) {
  return (
    <button
      type="button"
      className="btn btn-ghost btn-sm"
      aria-label="Close details"
      onClick={() => {
        onSelect(undefined);
      }}
    >
      ✕
    </button>
  );
}
