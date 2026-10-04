/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "../app/App";
import {
  demoDiff,
  demoGraph,
  demoScopes,
  emptyDiff,
  problem,
  respondByPath,
  respondWithJson,
  scopeSummary,
} from "../test/http";
import { renderAt } from "../test/render";

const DRIFT = "/scopes/webshop/PROD/k8s-prod-eu1/drift";

function requestPaths(fetch: ReturnType<typeof respondByPath>): string[] {
  return fetch.mock.calls.flatMap(([input]) =>
    input instanceof Request ? [new URL(input.url).pathname + new URL(input.url).search] : [],
  );
}

function jsonLink(): string {
  return screen.getByRole("link", { name: "Open as JSON" }).getAttribute("href") ?? "";
}

function rail() {
  return within(screen.getByRole("complementary", { name: "Context" }));
}

describe("DriftPage", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      respondByPath({
        "/diff/environments": demoDiff,
        "/diff/timeline": demoDiff,
        "/graph": demoGraph,
        "/scopes": demoScopes,
      }),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("picks a default left side, asks the control plane for the diff and lists it grouped", async () => {
    const fetch = respondByPath({ "/diff/environments": demoDiff, "/scopes": demoScopes });
    vi.stubGlobal("fetch", fetch);
    renderAt(DRIFT, <App />);

    expect(screen.getByRole("status")).toHaveTextContent("Loading the scopes…");
    expect(await screen.findByText("search-service")).toBeInTheDocument();
    expect(requestPaths(fetch)).toContain(
      "/api/v1/scopes/webshop/PROD/k8s-prod-eu1/diff/environments?leftEnvironment=DEV&leftCluster=k8s-dev-ci",
    );
    expect(jsonLink()).toBe(
      "/api/v1/scopes/webshop/PROD/k8s-prod-eu1/diff/environments?leftEnvironment=DEV&leftCluster=k8s-dev-ci",
    );
    expect(screen.getByRole("link", { name: "Drift" })).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("link", { name: "Map" })).toHaveAttribute(
      "href",
      "/scopes/webshop/PROD/k8s-prod-eu1",
    );
    expect(screen.getByRole("combobox", { name: "Left side" })).toHaveDisplayValue(
      "DEV · k8s-dev-ci",
    );
    expect(screen.getByLabelText("Right side")).toHaveTextContent("PROD · k8s-prod-eu1");
    expect(screen.getByText(/Right side relative to left/)).toHaveTextContent(
      "“only in PROD · k8s-prod-eu1” is new on the right",
    );

    const counters = within(screen.getByRole("list", { name: "Differences" }));
    expect(counters.getAllByRole("listitem").map((item) => item.textContent)).toEqual([
      "+2nodes only in PROD · k8s-prod-eu1",
      "−2nodes only in DEV · k8s-dev-ci",
      "Δ1changed nodes",
      "+2dependencies only in PROD · k8s-prod-eu1",
      "−2dependencies only in DEV · k8s-dev-ci",
    ]);
    const services = screen.getByRole("group", { name: /Services/ });
    expect(services).toHaveTextContent("1 added · 1 removed · 1 changed");
    expect(services).toHaveTextContent("exists only in DEV · k8s-dev-ci · v2.3.0");
    expect(services).toHaveTextContent(
      "v2.9.0 in DEV · k8s-dev-ci → v2.8.1 in PROD · k8s-prod-eu1",
    );
    expect(screen.getByRole("group", { name: /External/ })).toHaveTextContent("No differences.");
    expect(screen.getByRole("group", { name: /Dependencies/ })).toHaveTextContent(
      "search-service → postgresql/orders",
    );

    expect(
      rail().getByText("9 differences · 2 + 2 nodes, 1 changed, 4 dependencies"),
    ).toBeInTheDocument();
    expect(rail().getByText(/missing from PROD · k8s-prod-eu1/)).toBeInTheDocument();
    expect(rail().getByText("4 dependencies differ")).toBeInTheDocument();
  });

  it("switches the left side, swaps the sides and keeps the comparison in the URL", async () => {
    const user = userEvent.setup();
    const fetch = respondByPath({ "/diff/environments": demoDiff, "/scopes": demoScopes });
    vi.stubGlobal("fetch", fetch);
    renderAt(DRIFT, <App />);
    await screen.findByText("search-service");

    await user.selectOptions(
      screen.getByRole("combobox", { name: "Left side" }),
      "DEV · k8s-dev-eu1",
    );
    await waitFor(() => {
      expect(jsonLink()).toContain("leftEnvironment=DEV&leftCluster=k8s-dev-eu1");
    });
    expect(requestPaths(fetch)).toContain(
      "/api/v1/scopes/webshop/PROD/k8s-prod-eu1/diff/environments?leftEnvironment=DEV&leftCluster=k8s-dev-eu1",
    );

    await user.click(screen.getByRole("button", { name: "Swap sides" }));
    await waitFor(() => {
      expect(jsonLink()).toBe(
        "/api/v1/scopes/webshop/DEV/k8s-dev-eu1/diff/environments?leftEnvironment=PROD&leftCluster=k8s-prod-eu1",
      );
    });
    expect(screen.getByLabelText("Right side")).toHaveTextContent("DEV · k8s-dev-eu1");
    expect(screen.getByRole("navigation", { name: "Scope" })).toHaveTextContent(
      /webshop.*DEV.*k8s-dev-eu1/,
    );
  });

  it("shows a difference on the map with ghosts, flags, a drift legend and the selection in the rail", async () => {
    const user = userEvent.setup();
    renderAt(DRIFT, <App />);
    await screen.findByText("search-service");

    const row = screen.getByText("search-service").closest(".diff-row");
    expect(row).not.toBeNull();
    await user.click(within(row as HTMLElement).getByRole("button", { name: "Show on map" }));

    const canvas = await screen.findByRole("region", { name: "Service map canvas" });
    await waitFor(() => {
      expect(canvas.querySelectorAll(".react-flow__node")).toHaveLength(demoGraph.nodes.length + 2);
    });
    expect(canvas.querySelector(".react-flow__node.drift-removed")).not.toBeNull();
    expect(canvas.querySelector(".react-flow__node.drift-added")).not.toBeNull();
    expect(canvas.querySelector(".react-flow__node.drift-changed")).not.toBeNull();
    expect(within(canvas).getByText("v2.9.0 → v2.8.1")).toBeInTheDocument();
    expect(within(canvas).getAllByLabelText("removed")).toHaveLength(2);
    expect(screen.getByRole("group", { name: "Legend" })).toHaveTextContent(
      "only in DEV · k8s-dev-ci (ghost)",
    );
    await waitFor(() => {
      expect(canvas.querySelectorAll(".react-flow__edge.drift-removed")).toHaveLength(2);
    });
    expect(screen.getByRole("button", { name: "Map" })).toHaveAttribute("aria-pressed", "true");

    const selected = rail().getByRole("region", { name: "Selected" });
    expect(selected).toHaveTextContent("search-service");
    expect(selected).toHaveTextContent("Exists only in DEV · k8s-dev-ci.");
    expect(within(canvas).getByRole("button", { name: /search-service/ })).toHaveAttribute(
      "aria-pressed",
      "true",
    );

    fireEvent.click(within(canvas).getByRole("button", { name: /api-gateway/ }));
    expect(rail().getByRole("region", { name: "Selected" })).toHaveTextContent(
      "Identical on both sides.",
    );
    await user.click(rail().getByRole("button", { name: "Clear selection" }));
    expect(rail().queryByRole("region", { name: "Selected" })).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "List" }));
    expect(await screen.findByRole("group", { name: /Dependencies/ })).toBeInTheDocument();
  });

  it("compares a scope with its own past in the timeline mode", async () => {
    const user = userEvent.setup();
    const fetch = respondByPath({
      "/diff/environments": demoDiff,
      "/diff/timeline": demoDiff,
      "/scopes": demoScopes,
    });
    vi.stubGlobal("fetch", fetch);
    vi.useFakeTimers({ shouldAdvanceTime: true, now: new Date("2026-10-01T12:00:00Z") });
    try {
      renderAt(DRIFT, <App />);
      await screen.findByText("search-service");
      await user.click(screen.getByRole("button", { name: "Timeline" }));

      await waitFor(() => {
        expect(jsonLink()).toBe(
          "/api/v1/scopes/webshop/PROD/k8s-prod-eu1/diff/timeline?from=2026-09-30T12%3A00%3A00.000Z",
        );
      });
      expect(requestPaths(fetch)).toContain(
        "/api/v1/scopes/webshop/PROD/k8s-prod-eu1/diff/timeline?from=2026-09-30T12%3A00%3A00.000Z",
      );
      expect(
        screen.getByRole("button", { name: /From 2026-09-30 12:00:00 UTC/ }),
      ).toBeInTheDocument();
      expect(screen.getByRole("button", { name: "To now" })).toBeInTheDocument();
      expect(screen.getByLabelText("Both sides")).toHaveTextContent("PROD · k8s-prod-eu1");
      expect(screen.getByText(/Right side relative to left/)).toHaveTextContent(
        "“only in PROD · now”",
      );

      await user.click(screen.getByRole("button", { name: "To now" }));
      await user.type(screen.getByLabelText("Graph at (UTC)"), "2026-10-01T11:00:00");
      await user.click(screen.getByRole("button", { name: "Apply" }));
      await waitFor(() => {
        expect(jsonLink()).toContain("to=2026-10-01T11%3A00%3A00.000Z");
      });
      expect(screen.getByText(/Right side relative to left/)).toHaveTextContent(
        "“only in PROD · 2026-10-01 11:00:00 UTC”",
      );

      await user.click(screen.getByRole("button", { name: "Environments" }));
      await waitFor(() => {
        expect(jsonLink()).toContain(
          "/diff/environments?leftEnvironment=DEV&leftCluster=k8s-dev-ci",
        );
      });
    } finally {
      vi.useRealTimers();
    }
  });

  it("explains a project with a single scope, an aligned comparison and a failing control plane", async () => {
    vi.stubGlobal(
      "fetch",
      respondByPath({ "/scopes": [scopeSummary("billing", "PROD", "k8s-prod-eu1")] }),
    );
    const { unmount } = renderAt("/scopes/billing/PROD/k8s-prod-eu1/drift", <App />);
    expect(
      await screen.findByText(/billing has only one scope, so there is no other environment/),
    ).toBeInTheDocument();
    expect(screen.queryByRole("list", { name: "Differences" })).not.toBeInTheDocument();
    unmount();

    vi.stubGlobal(
      "fetch",
      respondByPath({ "/diff/environments": emptyDiff, "/scopes": demoScopes }),
    );
    const aligned = renderAt(`${DRIFT}?left=DEV&leftCluster=k8s-dev-eu1`, <App />);
    expect(await screen.findByText("No drift.")).toBeInTheDocument();
    expect(rail().getByText("Both sides are identical")).toBeInTheDocument();
    expect(rail().getByText("No differences")).toBeInTheDocument();
    aligned.unmount();

    vi.stubGlobal("fetch", respondWithJson(problem, 404, "application/problem+json"));
    renderAt(`${DRIFT}?left=DEV&leftCluster=k8s-dev-eu1`, <App />);
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "no agent has registered for scope webshop/PROD/k8s-prod-eu2",
    );
  });

  it("reports a graph that cannot be loaded for the map view", async () => {
    vi.stubGlobal(
      "fetch",
      respondByPath({ "/diff/environments": demoDiff, "/scopes": demoScopes }),
    );
    renderAt(`${DRIFT}?left=DEV&leftCluster=k8s-dev-eu1&view=map`, <App />);
    expect(await screen.findByRole("alert")).toHaveTextContent("The control plane did not answer");
    expect(screen.getByRole("list", { name: "Differences" })).toBeInTheDocument();
  });
});
