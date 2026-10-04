/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import type { ReactNode } from "react";
import { NavLink } from "react-router";
import { useTheme } from "./theme";

interface ShellProps {
  readonly title: string;
  readonly tools?: ReactNode;
  readonly apiRequest: string;
  readonly mapPath?: string;
  readonly children: ReactNode;
}

export function Shell({ title, tools, apiRequest, mapPath, children }: ShellProps) {
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
        {mapPath === undefined ? (
          <span
            className="rail-item rail-item-disabled"
            aria-disabled="true"
            title="Open a scope first"
          >
            Map
          </span>
        ) : (
          <NavLink className="rail-item" to={mapPath}>
            Map
          </NavLink>
        )}
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
        <a
          className="btn btn-ghost btn-sm topbar-api"
          href={apiRequest}
          target="_blank"
          rel="noreferrer"
        >
          Open as JSON
        </a>
      </header>
      <main className="page">{children}</main>
    </div>
  );
}
