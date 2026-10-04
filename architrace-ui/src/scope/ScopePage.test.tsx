/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "../app/App";
import { demoGraph, emptyGraph, problem, requestOf, respondWithJson } from "../test/http";
import { renderAt } from "../test/render";

const SCOPE = "/scopes/web%20shop/PROD/k8s-prod-eu1";

function chips() {
  return within(screen.getByRole("group", { name: "Node types" }));
}

describe("ScopePage", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", respondWithJson(demoGraph));
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("draws the graph of the scope with node cards, type chips, a count line and a legend", async () => {
    const fetch = respondWithJson(demoGraph);
    vi.stubGlobal("fetch", fetch);
    renderAt(SCOPE, <App />);

    expect(screen.getByRole("status")).toHaveTextContent("Loading the graph…");
    const breadcrumb = screen.getByRole("navigation", { name: "Scope" });
    expect(breadcrumb).toHaveTextContent("Projects/web shop/PROD/k8s-prod-eu1");
    expect(within(breadcrumb).getByRole("link", { name: "Projects" })).toHaveAttribute("href", "/");
    expect(screen.getByRole("link", { name: "Open as JSON" })).toHaveAttribute(
      "href",
      "/api/v1/scopes/web%20shop/PROD/k8s-prod-eu1/graph",
    );
    expect(screen.getByRole("link", { name: "Map" })).toHaveAttribute("aria-current", "page");

    expect(await screen.findByText("orders-service")).toBeInTheDocument();
    expect(new URL(requestOf(fetch).url).pathname).toBe(
      "/api/v1/scopes/web%20shop/PROD/k8s-prod-eu1/graph",
    );
    expect(screen.getByText("v2.8.1")).toBeInTheDocument();
    expect(screen.getByText("v4.1.2, v4.1.3")).toBeInTheDocument();
    expect(screen.getByText("notify")).toBeInTheDocument();
    expect(screen.getByText("db:postgresql/orders")).toBeInTheDocument();
    expect(screen.getByTitle("external:api.stripe.com")).toHaveTextContent("api.stripe.com");
    expect(chips().getByRole("button", { name: "Services 5" })).toHaveAttribute(
      "aria-pressed",
      "true",
    );
    expect(chips().getByRole("button", { name: "Data stores 2" })).toBeInTheDocument();
    expect(chips().getByRole("button", { name: "Data streams 2" })).toBeInTheDocument();
    expect(chips().getByRole("button", { name: "External 1" })).toBeInTheDocument();
    expect(
      screen.getByText(
        "5 services · 2 data stores · 2 data streams · 1 external host · 11 dependencies (7 sync, 4 stream)",
      ),
    ).toBeInTheDocument();
    expect(screen.getByRole("group", { name: "Legend" })).toHaveTextContent("publish / consume");
    expect(screen.getByRole("button", { name: /fit view/i })).toBeInTheDocument();
    await waitFor(() => {
      expect(document.querySelectorAll(".react-flow__edge")).toHaveLength(11);
    });
    expect(document.querySelectorAll(".react-flow__edge.health-bad")).toHaveLength(1);
    expect(document.querySelectorAll(".react-flow__edge.health-warn")).toHaveLength(1);
    expect(document.querySelectorAll(".react-flow__edge.edge-publish")).toHaveLength(2);
    expect(document.querySelectorAll(".react-flow__edge.edge-consume")).toHaveLength(2);
  });

  it("hides node types from the URL and toggles them with the chips", async () => {
    const user = userEvent.setup();
    renderAt(`${SCOPE}?hide=DATABASE`, <App />);

    expect(await screen.findByText("orders-service")).toBeInTheDocument();
    expect(screen.queryByText("postgresql/orders")).not.toBeInTheDocument();
    expect(chips().getByRole("button", { name: "Data stores 2" })).toHaveAttribute(
      "aria-pressed",
      "false",
    );
    expect(
      screen.getByText(
        "5 services · 0 data stores · 2 data streams · 1 external host · 9 dependencies (5 sync, 4 stream)",
      ),
    ).toBeInTheDocument();

    await user.click(chips().getByRole("button", { name: "Data stores 2" }));
    expect(await screen.findByText("postgresql/orders")).toBeInTheDocument();
    expect(chips().getByRole("button", { name: "Data stores 2" })).toHaveAttribute(
      "aria-pressed",
      "true",
    );

    await user.click(chips().getByRole("button", { name: "Services 5" }));
    expect(
      await screen.findByText(
        "0 services · 2 data stores · 2 data streams · 1 external host · 0 dependencies (0 sync, 0 stream)",
      ),
    ).toBeInTheDocument();
    expect(screen.queryByText("orders-service")).not.toBeInTheDocument();
  });

  it("says when every node type is hidden", async () => {
    renderAt(`${SCOPE}?hide=SERVICE&hide=DATABASE&hide=TOPIC&hide=EXTERNAL`, <App />);

    expect(await screen.findByText(/Every node type is hidden/)).toBeInTheDocument();
    expect(screen.queryByRole("region", { name: "Service map canvas" })).not.toBeInTheDocument();
  });

  it("explains a scope without snapshots", async () => {
    vi.stubGlobal("fetch", respondWithJson(emptyGraph));
    renderAt(SCOPE, <App />);

    expect(
      await screen.findByText(/No snapshot has reached the control plane/),
    ).toBeInTheDocument();
    expect(screen.queryByRole("group", { name: "Node types" })).not.toBeInTheDocument();
  });

  it("shows the problem the control plane answered", async () => {
    vi.stubGlobal("fetch", respondWithJson(problem, 404, "application/problem+json"));
    renderAt(SCOPE, <App />);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The control plane did not answer: no agent has registered for scope webshop/PROD/k8s-prod-eu2",
    );
  });
});
