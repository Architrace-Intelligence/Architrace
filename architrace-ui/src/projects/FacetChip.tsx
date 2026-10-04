/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { useRef } from "react";
import { usePopover } from "../app/usePopover";
import type { FacetValue } from "./filters";

interface FacetChipProps {
  readonly label: string;
  readonly values: readonly FacetValue[];
  readonly onToggle: (value: string) => void;
}

export function FacetChip({ label, values, onToggle }: FacetChipProps) {
  const container = useRef<HTMLSpanElement>(null);
  const popover = usePopover(container);
  const selected = values.filter((value) => value.selected);

  return (
    <span className="facet" ref={container}>
      <button
        type="button"
        className={selected.length > 0 ? "chip chip-on" : "chip"}
        aria-expanded={popover.open}
        aria-controls={popover.id}
        onClick={popover.toggle}
      >
        {label}
        {selected.length > 0 && (
          <span className="mono chip-value">{selected.map((value) => value.value).join(", ")}</span>
        )}
      </button>
      {popover.open && (
        <div
          className="popover"
          id={popover.id}
          role="group"
          aria-label={`Filter by ${label.toLowerCase()}`}
        >
          {values.length === 0 && <span className="faint pop-empty">No values</span>}
          {values.map((value) => (
            <button
              key={value.value}
              type="button"
              className={value.count === 0 && !value.selected ? "pop-item faint" : "pop-item"}
              aria-pressed={value.selected}
              onClick={() => {
                onToggle(value.value);
              }}
            >
              <span className="mono">{value.value}</span>{" "}
              <span className="pop-count">{value.count}</span>
            </button>
          ))}
        </div>
      )}
    </span>
  );
}
