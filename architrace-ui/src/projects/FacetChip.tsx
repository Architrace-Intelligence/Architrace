/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { useEffect, useId, useRef, useState } from "react";
import type { FacetValue } from "./filters";

interface FacetChipProps {
  readonly label: string;
  readonly values: readonly FacetValue[];
  readonly onToggle: (value: string) => void;
}

export function FacetChip({ label, values, onToggle }: FacetChipProps) {
  const [open, setOpen] = useState(false);
  const container = useRef<HTMLSpanElement>(null);
  const popoverId = useId();
  const selected = values.filter((value) => value.selected);

  useEffect(() => {
    if (!open) {
      return undefined;
    }
    const close = (event: MouseEvent | KeyboardEvent) => {
      if (
        event instanceof KeyboardEvent
          ? event.key === "Escape"
          : !container.current?.contains(event.target as Node)
      ) {
        setOpen(false);
      }
    };
    document.addEventListener("mousedown", close);
    document.addEventListener("keydown", close);
    return () => {
      document.removeEventListener("mousedown", close);
      document.removeEventListener("keydown", close);
    };
  }, [open]);

  return (
    <span className="facet" ref={container}>
      <button
        type="button"
        className={selected.length > 0 ? "chip chip-on" : "chip"}
        aria-expanded={open}
        aria-controls={popoverId}
        onClick={() => {
          setOpen((current) => !current);
        }}
      >
        {label}
        {selected.length > 0 && (
          <span className="mono chip-value">{selected.map((value) => value.value).join(", ")}</span>
        )}
      </button>
      {open && (
        <div
          className="popover"
          id={popoverId}
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
