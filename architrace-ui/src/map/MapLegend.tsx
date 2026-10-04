/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { NODE_TYPE_LABELS, NODE_TYPE_TOKENS, NODE_TYPES } from "./model";

export function MapLegend() {
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
      </div>
    </fieldset>
  );
}
