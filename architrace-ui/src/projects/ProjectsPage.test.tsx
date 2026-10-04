/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { demoScopes, problem, respondWithJson } from "../test/http";
import { renderAt } from "../test/render";
import { ProjectsPage } from "./ProjectsPage";

function chip(label: string) {
  return screen.getByRole("button", { name: new RegExp(`^${label}`), expanded: false });
}

function groupButton(label: string) {
  return within(screen.getByRole("group", { name: "Group by" })).getByRole("button", {
    name: label,
  });
}

describe("ProjectsPage", () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ["Date"], now: new Date("2026-10-01T12:01:00Z") });
    vi.stubGlobal("fetch", respondWithJson(demoScopes));
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.useRealTimers();
  });

  it("lists every scope grouped by project with counts, agents and relative snapshot age", async () => {
    renderAt("/", <ProjectsPage />);

    expect(screen.getByRole("status")).toHaveTextContent("Connecting to the control plane…");
    const webshop = await screen.findByRole("region", { name: "webshop" });
    expect(screen.getByRole("heading", { name: "Projects" })).toBeInTheDocument();
    expect(screen.getByRole("region", { name: "billing" })).toBeInTheDocument();
    expect(
      within(webshop).getByText("2 environments · 4 clusters · 18 services · 8 data streams"),
    ).toBeInTheDocument();
    expect(within(webshop).getAllByRole("row")).toHaveLength(5);
    const prodRow = within(webshop).getAllByRole("link", { name: "PROD" })[0]?.closest("tr");
    expect(prodRow).not.toBeNull();
    expect(prodRow).toHaveTextContent("k8s-prod-eu1");
    expect(prodRow).toHaveTextContent("2 live");
    expect(prodRow).toHaveTextContent("1 min ago");
    expect(within(webshop).getByText("stale · 1")).toBeInTheDocument();
    expect(within(webshop).getByText("2 h ago")).toBeInTheDocument();
    expect(screen.getByText("—")).toBeInTheDocument();
    expect(
      screen.getByText("6 scopes · 2 projects · 3 environments · 5 clusters · 6 agents, 1 stale"),
    ).toBeInTheDocument();
    expect(within(webshop).getAllByRole("link", { name: "PROD" })[0]).toHaveAttribute(
      "href",
      "/scopes/webshop/PROD/k8s-prod-eu1",
    );
    expect(screen.getByRole("link", { name: "Open as JSON" })).toHaveAttribute(
      "href",
      "/api/v1/scopes",
    );
  });

  it("reads the filter from the URL and lets the chips change it", async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime.bind(vi) });
    renderAt("/?env=PROD&group=cluster", <ProjectsPage />);

    expect(await screen.findByRole("region", { name: "k8s-prod-eu1" })).toBeInTheDocument();
    expect(
      screen.getByText("3 of 6 scopes · 2 projects · 1 environment · 2 clusters · 4 agents"),
    ).toBeInTheDocument();
    expect(chip("Environment")).toHaveTextContent("PROD");
    expect(chip("Environment")).toHaveClass("chip-on");
    expect(groupButton("Cluster")).toHaveAttribute("aria-pressed", "true");

    await user.click(chip("Cluster"));
    const popover = screen.getByRole("group", { name: "Filter by cluster" });
    expect(within(popover).getByRole("button", { name: "k8s-prod-eu2 1" })).toHaveAttribute(
      "aria-pressed",
      "false",
    );
    expect(within(popover).getByRole("button", { name: "k8s-dev-ci 0" })).toHaveClass("faint");
    await user.click(within(popover).getByRole("button", { name: "k8s-prod-eu2 1" }));

    expect(
      await screen.findByText("1 of 6 scopes · 1 project · 1 environment · 1 cluster · 1 agent"),
    ).toBeInTheDocument();
    expect(screen.queryByRole("region", { name: "k8s-prod-eu1" })).not.toBeInTheDocument();

    await user.keyboard("{Escape}");
    expect(screen.queryByRole("group", { name: "Filter by cluster" })).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Clear all" }));
    expect(
      await screen.findByText(
        "6 scopes · 2 projects · 3 environments · 5 clusters · 6 agents, 1 stale",
      ),
    ).toBeInTheDocument();
    expect(groupButton("Cluster")).toHaveAttribute("aria-pressed", "true");
  });

  it("switches the grouping and searches by text", async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime.bind(vi) });
    renderAt("/", <ProjectsPage />);
    await screen.findByRole("region", { name: "webshop" });

    await user.click(groupButton("Environment"));
    const prod = await screen.findByRole("region", { name: "PROD" });
    expect(within(prod).getByRole("columnheader", { name: "Project" })).toBeInTheDocument();

    await user.type(screen.getByRole("searchbox", { name: "Filter scopes" }), "bill");
    expect(
      await screen.findByText("2 of 6 scopes · 1 project · 2 environments · 2 clusters · 1 agent"),
    ).toBeInTheDocument();
    expect(screen.queryByRole("region", { name: "DEV" })).not.toBeInTheDocument();

    await user.clear(screen.getByRole("searchbox", { name: "Filter scopes" }));
    await user.type(screen.getByRole("searchbox", { name: "Filter scopes" }), "nowhere");
    expect(await screen.findByText("No scope matches the filter.")).toBeInTheDocument();
  });

  it("closes the popover when clicking elsewhere", async () => {
    const user = userEvent.setup({ advanceTimers: vi.advanceTimersByTime.bind(vi) });
    renderAt("/", <ProjectsPage />);
    await screen.findByRole("region", { name: "webshop" });

    await user.click(chip("Environment"));
    expect(screen.getByRole("group", { name: "Filter by environment" })).toBeInTheDocument();
    await user.click(screen.getByRole("heading", { name: "Projects" }));

    expect(screen.queryByRole("group", { name: "Filter by environment" })).not.toBeInTheDocument();
  });

  it("explains an empty control plane", async () => {
    vi.stubGlobal("fetch", respondWithJson([]));
    renderAt("/", <ProjectsPage />);

    expect(await screen.findByText(/No agent has registered yet/)).toBeInTheDocument();
    expect(
      screen.getByText("0 scopes · 0 projects · 0 environments · 0 clusters · 0 agents"),
    ).toBeInTheDocument();
  });

  it("shows the problem the control plane answered", async () => {
    vi.stubGlobal("fetch", respondWithJson(problem, 404, "application/problem+json"));
    renderAt("/", <ProjectsPage />);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The control plane did not answer: no agent has registered for scope webshop/PROD/k8s-prod-eu2",
    );
  });

  it("shows a failure to reach the control plane", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() => Promise.reject(new TypeError("Failed to fetch"))),
    );
    renderAt("/", <ProjectsPage />);

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The control plane did not answer: Failed to fetch",
    );
  });
});
