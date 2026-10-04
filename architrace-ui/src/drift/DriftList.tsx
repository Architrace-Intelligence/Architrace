/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { GLYPHS } from "../map/model";
import type { DiffGroup } from "./model";

interface DriftListProps {
  readonly groups: readonly DiffGroup[];
  readonly onShowOnMap: (nodeId: string) => void;
}

export function DriftList({ groups, onShowOnMap }: DriftListProps) {
  const aligned = groups.every((group) => group.rows.length === 0);
  return (
    <>
      {aligned && (
        <p className="empty">
          <b>No drift.</b> Both sides have the same nodes, versions and dependencies.
        </p>
      )}
      {groups.map((group) => (
        <details
          className="group"
          key={group.id}
          open={group.rows.length > 0}
          aria-label={group.label}
        >
          <summary className="group-h">
            <h3>{group.label}</h3>
            <span className="badge b-kind">{group.rows.length}</span>
            <span className="group-meta">{group.summary}</span>
          </summary>
          {group.rows.length === 0 && <p className="diff-row faint">No differences.</p>}
          {group.rows.map((row) => (
            <div className="diff-row" key={row.key}>
              <span className={`glyph b-${row.kind}`} aria-label={row.kind}>
                {GLYPHS[row.kind]}
              </span>
              <span className="diff-text">
                <b>{row.name}</b>
                <span>{row.detail}</span>
              </span>
              <button
                type="button"
                className="btn btn-sm"
                onClick={() => {
                  onShowOnMap(row.nodeId);
                }}
              >
                Show on map
              </button>
            </div>
          ))}
        </details>
      ))}
    </>
  );
}
