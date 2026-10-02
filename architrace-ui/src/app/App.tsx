/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { ControlPlaneStatus } from "./ControlPlaneStatus";

export function App() {
  return (
    <div className="shell">
      <header className="topbar">
        <span className="wordmark">Architrace</span>
        <h1 className="title">Projects</h1>
      </header>
      <main className="content">
        <ControlPlaneStatus />
      </main>
    </div>
  );
}
