/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "../app/App";
import { demoGraph, emptyGraph, problem, requestOf, respondWithJson } from "../test/http";
import { renderAt } from "../test/render";

const SCOPE = "/scopes/web%20shop/PROD/k8s-prod-eu1";

function chips() {
  return within(screen.getByRole("group", { name: "Node types" }));
}

function rail() {
  return within(screen.getByRole("complementary", { name: "Context" }));
}

function element(selector: string): Element {
  const found = document.querySelector(selector);
  if (found === null) {
    throw new Error(`${selector} is not on the page`);
  }
  return found;
}

function count(selector: string): number {
  return document.querySelectorAll(selector).length;
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

  it("selects a node from the URL, lights its neighbourhood and shows it in the context rail", async () => {
    renderAt(`${SCOPE}?node=service%3Aorders-service`, <App />);

    expect(await screen.findByText("Service · PROD")).toBeInTheDocument();
    expect(rail().getByText("orders-service")).toBeInTheDocument();
    await waitFor(() => {
      expect(count(".react-flow__node.selected")).toBe(1);
    });
    expect(count(".react-flow__node.dimmed")).toBe(5);
    expect(count(".react-flow__edge.touching")).toBe(5);
    expect(count(".react-flow__edge.dimmed")).toBe(6);
    expect(screen.getAllByText("12.4k · 0.3 %").length).toBeGreaterThan(1);
  });

  it("selects an edge from the URL and describes the dependency", async () => {
    renderAt(
      `${SCOPE}?edge=service%3Apayments-service%3Eexternal%3Aapi.stripe.com%3ASYNC`,
      <App />,
    );

    expect(await screen.findByText("Dependency · PROD")).toBeInTheDocument();
    expect(rail().getByText("payments-service → api.stripe.com")).toBeInTheDocument();
    await waitFor(() => {
      expect(count(".react-flow__edge.touching")).toBe(1);
    });
    expect(count(".react-flow__node.dimmed")).toBe(8);
  });

  it("selects by click and keyboard, clears with the pane and with Escape", async () => {
    const user = userEvent.setup();
    renderAt(SCOPE, <App />);
    await screen.findByText("Scope · PROD");

    fireEvent.click(await screen.findByText("payments-service"));
    expect(await screen.findByText("Service · PROD")).toBeInTheDocument();
    expect(rail().getByText("service:payments-service")).toBeInTheDocument();

    fireEvent.click(element(".react-flow__pane"));
    expect(await screen.findByText("Scope · PROD")).toBeInTheDocument();

    const card = element('.react-flow__node[data-id="service:api-gateway"]');
    if (card instanceof HTMLElement) {
      card.focus();
    }
    await user.keyboard("{Enter}");
    expect(await screen.findByText("service:api-gateway")).toBeInTheDocument();

    await user.keyboard("{Escape}");
    expect(await screen.findByText("Scope · PROD")).toBeInTheDocument();

    await user.click(rail().getByRole("button", { name: /order-events/ }));
    expect(await screen.findByText("Data stream · PROD")).toBeInTheDocument();
  });

  it("switches the lens, finds nodes and asks for a point in time", async () => {
    const user = userEvent.setup();
    const fetch = respondWithJson(demoGraph);
    vi.stubGlobal("fetch", fetch);
    renderAt(SCOPE, <App />);
    await screen.findByText("orders-service");

    await user.click(
      within(screen.getByRole("group", { name: "Lens" })).getByRole("button", {
        name: "Data streams",
      }),
    );
    await waitFor(() => {
      expect(count(".react-flow__node.dimmed")).toBe(5);
    });
    expect(count(".react-flow__edge.dimmed")).toBe(7);

    await user.type(screen.getByRole("searchbox", { name: "Find in map" }), "pay");
    expect(await screen.findByText("3 of 10 nodes match “pay”")).toBeInTheDocument();
    await waitFor(() => {
      expect(count(".react-flow__node.match")).toBe(3);
    });

    await user.click(screen.getByRole("button", { name: "Live" }));
    const form = screen.getByRole("form", { name: "Point in time" });
    fireEvent.change(within(form).getByLabelText("Graph at (UTC)"), {
      target: { value: "2026-10-01T12:00:00" },
    });
    await user.click(within(form).getByRole("button", { name: "Apply" }));

    expect(
      await screen.findByRole("button", { name: "At 2026-10-01 12:00:00 UTC" }),
    ).toBeInTheDocument();
    await waitFor(() => {
      expect(fetch).toHaveBeenCalledTimes(2);
    });
    const request = fetch.mock.calls[1]?.[0];
    expect(request instanceof Request ? new URL(request.url).search : "").toBe(
      "?at=2026-10-01T12%3A00%3A00.000Z",
    );
    expect(screen.getByRole("link", { name: "Open as JSON" })).toHaveAttribute(
      "href",
      "/api/v1/scopes/web%20shop/PROD/k8s-prod-eu1/graph?at=2026-10-01T12%3A00%3A00.000Z",
    );
  });
});
