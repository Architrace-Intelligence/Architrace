/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { GLYPHS, NODE_TYPE_LABELS, NODE_TYPE_TOKENS, NODE_TYPES, type SideLabels } from "./model";

interface MapLegendProps {
  readonly sides?: SideLabels;
  readonly impact?: boolean;
}

export function MapLegend({ sides, impact = false }: MapLegendProps) {
  return (
    <fieldset className="legend">
      <legend className="sr-only">Legend</legend>
      <div className="legend-col">
        {NODE_TYPES.map((type) => (
          <div className="legend-row" key={type}>
            <span className={`dot dot-${NODE_TYPE_TOKENS[type]}`} aria-hidden="true" />
            <span>{NODE_TYPE_LABELS[type].singular}</span>
          </div>
        ))}
      </div>
      {impact && <ImpactLegend />}
      {!impact && sides === undefined && <TrafficLegend />}
      {!impact && sides !== undefined && <DriftLegend sides={sides} />}
    </fieldset>
  );
}

function TrafficLegend() {
  return (
    <div className="legend-col">
      <div className="legend-row">
        <span className="line-sample" aria-hidden="true" />
        <span>sync call</span>
      </div>
      <div className="legend-row">
        <span className="line-sample line-dashed" aria-hidden="true" />
        <span>publish / consume</span>
      </div>
      <div className="legend-row">
        <span className="line-sample line-thick" aria-hidden="true" />
        <span>width grows with calls</span>
      </div>
      <div className="legend-row">
        <span className="line-sample line-warn" aria-hidden="true" />
        <span>errors ≥ 1 %</span>
      </div>
      <div className="legend-row">
        <span className="line-sample line-bad" aria-hidden="true" />
        <span>errors ≥ 3 %</span>
      </div>
      <div className="legend-row">
        <span className="badge node-badge sev-high" aria-hidden="true">
          2
        </span>
        <span>findings on the node</span>
      </div>
    </div>
  );
}

function ImpactLegend() {
  return (
    <div className="legend-col">
      <div className="legend-row">
        <span className="swatch swatch-subject" aria-hidden="true" />
        <span>if this node fails</span>
      </div>
      <div className="legend-row">
        <span className="swatch swatch-impaired" aria-hidden="true" />
        <span>impaired, one hop away</span>
      </div>
      <div className="legend-row">
        <span className="swatch swatch-far" aria-hidden="true" />
        <span>impaired further away</span>
      </div>
      <div className="legend-row">
        <span className="swatch swatch-delayed" aria-hidden="true" />
        <span>delayed, data arrives late</span>
      </div>
    </div>
  );
}

function DriftLegend({ sides }: { readonly sides: SideLabels }) {
  return (
    <div className="legend-col">
      <div className="legend-row">
        <span className="glyph glyph-sm b-added" aria-hidden="true">
          {GLYPHS.added}
        </span>
        <span>only in {sides.right}</span>
      </div>
      <div className="legend-row">
        <span className="glyph glyph-sm b-removed" aria-hidden="true">
          {GLYPHS.removed}
        </span>
        <span>only in {sides.left} (ghost)</span>
      </div>
      <div className="legend-row">
        <span className="glyph glyph-sm b-changed" aria-hidden="true">
          {GLYPHS.changed}
        </span>
        <span>version differs</span>
      </div>
      <div className="legend-row">
        <span className="line-sample line-added" aria-hidden="true" />
        <span>dependency only in {sides.right}</span>
      </div>
      <div className="legend-row">
        <span className="line-sample line-removed" aria-hidden="true" />
        <span>dependency only in {sides.left}</span>
      </div>
    </div>
  );
}
