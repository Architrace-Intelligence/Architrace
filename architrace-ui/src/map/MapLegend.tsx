/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { NODE_TYPE_LABELS, NODE_TYPE_TOKENS, NODE_TYPES } from "./model";

export function MapLegend() {
  return (
    <div className="legend" role="group" aria-label="Legend">
      <div className="legend-col">
        {NODE_TYPES.map((type) => (
          <div className="legend-row" key={type}>
            <span className={`dot dot-${NODE_TYPE_TOKENS[type]}`} aria-hidden="true" />
            {NODE_TYPE_LABELS[type].singular}
          </div>
        ))}
      </div>
      <div className="legend-col">
        <div className="legend-row">
          <span className="line-sample" aria-hidden="true" />
          sync call
        </div>
        <div className="legend-row">
          <span className="line-sample line-dashed" aria-hidden="true" />
          publish / consume
        </div>
        <div className="legend-row">
          <span className="line-sample line-thick" aria-hidden="true" />
          width grows with calls
        </div>
        <div className="legend-row">
          <span className="line-sample line-warn" aria-hidden="true" />
          errors ≥ 1 %
        </div>
        <div className="legend-row">
          <span className="line-sample line-bad" aria-hidden="true" />
          errors ≥ 3 %
        </div>
      </div>
    </div>
  );
}
