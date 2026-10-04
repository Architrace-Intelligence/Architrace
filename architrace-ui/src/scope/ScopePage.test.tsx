/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { screen, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { renderAt } from "../test/render";
import { App } from "../app/App";

describe("ScopePage", () => {
  it("shows the scope breadcrumb and the request behind the map", () => {
    renderAt("/scopes/web%20shop/PROD/k8s-prod-eu1", <App />);

    const breadcrumb = screen.getByRole("navigation", { name: "Scope" });
    expect(breadcrumb).toHaveTextContent("Projects/web shop/PROD/k8s-prod-eu1");
    expect(within(breadcrumb).getByRole("link", { name: "Projects" })).toHaveAttribute("href", "/");
    expect(
      screen.getByRole("link", { name: "/api/v1/scopes/web%20shop/PROD/k8s-prod-eu1/graph" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Open as JSON" })).toHaveAttribute(
      "href",
      "/api/v1/scopes/web%20shop/PROD/k8s-prod-eu1/graph",
    );
    expect(screen.getByRole("link", { name: "Map" })).toHaveAttribute("aria-current", "page");
  });
});
