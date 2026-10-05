/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { type ReactNode, useState } from "react";
import { NavLink } from "react-router";
import { useTheme } from "./theme";

interface ShellProps {
  readonly title: string;
  readonly tools?: ReactNode;
  readonly apiRequest: string;
  readonly scopePath?: string;
  readonly aside?: ReactNode;
  readonly children: ReactNode;
}

const COPIED_MILLIS = 1_500;

export function Shell({ title, tools, apiRequest, scopePath, aside, children }: ShellProps) {
  const [theme, toggleTheme] = useTheme();
  return (
    <div className="app">
      <nav className="rail" aria-label="Primary">
        <span className="rail-logo" aria-hidden="true">
          A
        </span>
        <NavLink className="rail-item" to="/" end>
          Projects
        </NavLink>
        <ScopeLink label="Map" to={scopePath} />
        <ScopeLink label="Drift" to={scopePath === undefined ? undefined : `${scopePath}/drift`} />
        <ScopeLink
          label="Findings"
          to={scopePath === undefined ? undefined : `${scopePath}/findings`}
        />
        <span className="rail-spacer" />
        <button
          type="button"
          className="rail-item"
          onClick={toggleTheme}
          aria-label={theme === "dark" ? "Switch to the light theme" : "Switch to the dark theme"}
        >
          Theme
        </button>
      </nav>
      <header className="topbar">
        <h1 className="title">{title}</h1>
        {tools}
        <span className="topbar-actions">
          <CopyLink />
          <a className="btn btn-ghost btn-sm" href={apiRequest} target="_blank" rel="noreferrer">
            Open as JSON
          </a>
        </span>
      </header>
      <div className="main">
        <main className="page">{children}</main>
        {aside !== undefined && (
          <aside className="panel" aria-label="Context">
            {aside}
          </aside>
        )}
      </div>
    </div>
  );
}

interface ScopeLinkProps {
  readonly label: string;
  readonly to: string | undefined;
}

function ScopeLink({ label, to }: ScopeLinkProps) {
  if (to === undefined) {
    return (
      <span
        className="rail-item rail-item-disabled"
        aria-disabled="true"
        title="Open a scope first"
      >
        {label}
      </span>
    );
  }
  return (
    <NavLink className="rail-item" to={to} end>
      {label}
    </NavLink>
  );
}

function CopyLink() {
  const [copied, setCopied] = useState(false);
  return (
    <button
      type="button"
      className="btn btn-ghost btn-sm"
      onClick={() => {
        void navigator.clipboard.writeText(window.location.href).then(() => {
          setCopied(true);
          window.setTimeout(() => {
            setCopied(false);
          }, COPIED_MILLIS);
        });
      }}
    >
      {copied ? "Copied" : "Copy link"}
    </button>
  );
}
