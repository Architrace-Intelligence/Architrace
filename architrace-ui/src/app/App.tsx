/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { Route, Routes } from "react-router";
import { DriftPage } from "../drift/DriftPage";
import { ProjectsPage } from "../projects/ProjectsPage";
import { ScopePage } from "../scope/ScopePage";

export function App() {
  return (
    <Routes>
      <Route path="/" element={<ProjectsPage />} />
      <Route path="/scopes/:project/:environment/:cluster" element={<ScopePage />} />
      <Route path="/scopes/:project/:environment/:cluster/drift" element={<DriftPage />} />
      <Route path="*" element={<ProjectsPage />} />
    </Routes>
  );
}
