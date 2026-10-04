/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import type { TopologyDiff, TopologyGraph } from "../api/client";
import { GLYPHS, type SideLabels } from "../map/model";
import { describeSelected, headline, summarise } from "./model";

interface DriftPanelProps {
  readonly diff: TopologyDiff;
  readonly sides: SideLabels;
  readonly selected: string | undefined;
  readonly graph: TopologyGraph | undefined;
  readonly onSelect: (nodeId: string | undefined) => void;
}

export function DriftPanel({ diff, sides, selected, graph, onSelect }: DriftPanelProps) {
  const node = selected === undefined ? undefined : describeSelected(diff, selected, sides, graph);
  return (
    <>
      <div className="panel-head">
        <span className="panel-kicker">Comparison</span>
        <div className="badges sides">
          <span className="badge b-kind">{sides.left}</span>
          <span aria-hidden="true">→</span>
          <span className="badge b-kind">{sides.right}</span>
        </div>
        <span className="muted">{headline(diff)}</span>
      </div>
      {node !== undefined && (
        <section className="panel-body panel-selected" aria-label="Selected">
          <div className="panel-row">
            <span className="panel-kicker">Selected</span>
            <button
              type="button"
              className="btn btn-ghost btn-sm"
              aria-label="Clear selection"
              onClick={() => {
                onSelect(undefined);
              }}
            >
              ✕
            </button>
          </div>
          <div className="panel-row">
            <span className={`glyph ${node.kind === "same" ? "b-kind" : `b-${node.kind}`}`}>
              {node.kind === "same" ? "=" : GLYPHS[node.kind]}
            </span>
            <b>{node.name}</b>
          </div>
          <span className="muted">{node.detail}</span>
        </section>
      )}
      <div className="panel-body">
        <span className="panel-kicker">Drift summary · deterministic</span>
        {summarise(diff, sides, graph).map((sentence) => (
          <article className="sentence" key={sentence.title}>
            <span className="panel-row">
              <span
                className={`badge ${sentence.kind === "aligned" ? "b-ok" : `b-${sentence.kind}`}`}
              >
                {sentence.badge}
              </span>
              <b>{sentence.title}</b>
            </span>
            <span className="muted">{sentence.body}</span>
          </article>
        ))}
        <p className="note">
          Nodes are matched by their environment-independent id. Traffic metrics are never compared:
          volume is not drift.
        </p>
      </div>
    </>
  );
}
