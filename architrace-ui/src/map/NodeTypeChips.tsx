/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import type { NodeType } from "../api/client";
import { type MapState, NODE_TYPE_LABELS, NODE_TYPE_TOKENS, NODE_TYPES } from "./model";

interface NodeTypeChipsProps {
  readonly counts: Record<NodeType, number>;
  readonly filter: MapState;
  readonly onToggle: (type: NodeType) => void;
}

export function NodeTypeChips({ counts, filter, onToggle }: NodeTypeChipsProps) {
  return (
    <fieldset className="chips">
      <legend className="sr-only">Node types</legend>
      {NODE_TYPES.map((type) => {
        const shown = !filter.hidden.includes(type);
        return (
          <button
            key={type}
            type="button"
            className={shown ? "chip" : "chip chip-off"}
            aria-pressed={shown}
            onClick={() => {
              onToggle(type);
            }}
          >
            <span className={`dot dot-${NODE_TYPE_TOKENS[type]}`} aria-hidden="true" />
            {NODE_TYPE_LABELS[type].plural}{" "}
            <span className="faint mono chip-count">{counts[type]}</span>
          </button>
        );
      })}
    </fieldset>
  );
}
