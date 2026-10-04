/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { Link, useParams } from "react-router";
import { Shell } from "../app/Shell";

export function ScopePage() {
  const { project = "", environment = "", cluster = "" } = useParams();
  const base = `/api/v1/scopes/${encodeURIComponent(project)}/${encodeURIComponent(environment)}/${encodeURIComponent(cluster)}`;
  const path = `/scopes/${encodeURIComponent(project)}/${encodeURIComponent(environment)}/${encodeURIComponent(cluster)}`;
  return (
    <Shell title="Service map" apiRequest={`${base}/graph`} mapPath={path}>
      <nav className="breadcrumb" aria-label="Scope">
        <Link to="/">Projects</Link>
        <span aria-hidden="true">/</span>
        <span>{project}</span>
        <span aria-hidden="true">/</span>
        <span className="badge b-kind">{environment}</span>
        <span aria-hidden="true">/</span>
        <span className="mono">{cluster}</span>
      </nav>
      <p className="empty">
        The service map of this scope arrives with the next release; the graph is already served at{" "}
        <a href={`${base}/graph`} target="_blank" rel="noreferrer" className="mono">
          {base}/graph
        </a>
        .
      </p>
    </Shell>
  );
}
