/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { useRef, useState } from "react";
import { usePopover } from "../app/usePopover";
import { formatInstant } from "./metrics";

interface TimeSelectorProps {
  readonly at: string | undefined;
  readonly onChange: (at: string | undefined) => void;
  readonly prefix?: string;
  readonly liveLabel?: string;
  readonly allowLive?: boolean;
  readonly formLabel?: string;
}

function toInputValue(at: string | undefined): string {
  if (at === undefined) {
    return "";
  }
  const millis = Date.parse(at);
  return Number.isNaN(millis) ? "" : new Date(millis).toISOString().slice(0, 19);
}

export function TimeSelector({
  at,
  onChange,
  prefix = "At",
  liveLabel = "Live",
  allowLive = true,
  formLabel = "Point in time",
}: TimeSelectorProps) {
  const container = useRef<HTMLSpanElement>(null);
  const popover = usePopover(container);
  const [draft, setDraft] = useState(() => toInputValue(at));
  const live = at === undefined;
  return (
    <span className="facet" ref={container}>
      <button
        type="button"
        className={live ? "btn btn-sm" : "btn btn-sm chip-on"}
        aria-expanded={popover.open}
        aria-controls={popover.id}
        onClick={() => {
          setDraft(toInputValue(at));
          popover.toggle();
        }}
      >
        <span className={live ? "dot dot-ok" : "dot dot-time"} aria-hidden="true" />
        {live ? liveLabel : `${prefix} ${formatInstant(at)}`}
      </button>
      {popover.open && (
        <form
          className="popover popover-form popover-right"
          id={popover.id}
          aria-label={formLabel}
          onSubmit={(event) => {
            event.preventDefault();
            onChange(new Date(`${draft}Z`).toISOString());
            popover.close();
          }}
        >
          <label>
            <span>Graph at (UTC)</span>
            <input
              type="datetime-local"
              step={1}
              required
              value={draft}
              onChange={(event) => {
                setDraft(event.target.value);
              }}
            />
          </label>
          <div className="popover-actions">
            {allowLive && (
              <button
                type="button"
                className="btn btn-ghost btn-sm"
                onClick={() => {
                  onChange(undefined);
                  popover.close();
                }}
              >
                {liveLabel}
              </button>
            )}
            <button type="submit" className="btn btn-sm" disabled={draft === ""}>
              Apply
            </button>
          </div>
        </form>
      )}
    </span>
  );
}
