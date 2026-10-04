/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { demoScopes, respondWithJson } from "../test/http";
import { renderAt } from "../test/render";
import { App } from "./App";

describe("App", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("opens the projects list at the root and for unknown paths", async () => {
    vi.stubGlobal("fetch", respondWithJson(demoScopes));

    const { unmount } = renderAt("/", <App />);
    expect(await screen.findByRole("region", { name: "webshop" })).toBeInTheDocument();
    unmount();

    renderAt("/nowhere", <App />);
    expect(screen.getByRole("heading", { name: "Projects" })).toBeInTheDocument();
  });
});
