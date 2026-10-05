/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { App } from "../app/App";
import {
  demoFindings,
  demoGraph,
  demoScopes,
  problem,
  respondByPath,
  respondWithJson,
} from "../test/http";
import { renderAt } from "../test/render";

const FINDINGS = "/scopes/webshop/PROD/k8s-prod-eu1/findings";

function jsonLink(): string {
  return screen.getByRole("link", { name: "Open as JSON" }).getAttribute("href") ?? "";
}

function rail() {
  return within(screen.getByRole("complementary", { name: "Context" }));
}

describe("FindingsPage", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      respondByPath({ "/findings": demoFindings, "/graph": demoGraph, "/scopes": demoScopes }),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("lists the findings grouped by rule, high severity first, with the rail totals", async () => {
    renderAt(FINDINGS, <App />);

    expect(screen.getByRole("status")).toHaveTextContent("Loading the findings…");
    expect(await screen.findByText("Cyclic dependency between 2 services")).toBeInTheDocument();
    const groups = screen.getAllByRole("group").map((group) => group.getAttribute("aria-label"));
    expect(groups.filter((label) => label !== null && label !== "Severity")).toEqual([
      "Cyclic dependency",
      "Wide blast radius",
      "Fan-in hub",
      "Unknown external",
    ]);
    expect(
      screen.getByText("Evaluated 2026-10-01 12:00:00 UTC · 4 of 4 findings shown"),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Findings" })).toHaveAttribute("aria-current", "page");
    expect(jsonLink()).toBe("/api/v1/scopes/webshop/PROD/k8s-prod-eu1/findings");
    const context = rail();
    expect(context.getByLabelText("Findings by severity")).toHaveTextContent("2high1medium1low");
    expect(context.getByText("Shared database").previousSibling).toHaveTextContent("pass");
    expect(context.getByText("Fan-in hub").previousSibling).toHaveTextContent("1 finding");
    expect(
      within(screen.getByRole("region", { name: "Rules that pass" })).getAllByText(/./, {
        selector: ".badge",
      }),
    ).toHaveLength(3);
  });

  it("filters by severity and rule, keeps the filter in the URL and in the JSON link", async () => {
    const user = userEvent.setup();
    renderAt(FINDINGS, <App />);
    await screen.findByText("Cyclic dependency between 2 services");

    await user.click(screen.getByRole("button", { name: /^medium/ }));
    expect(
      screen.getByText("Evaluated 2026-10-01 12:00:00 UTC · 1 of 4 findings shown"),
    ).toBeInTheDocument();
    expect(screen.queryByText("Cyclic dependency between 2 services")).not.toBeInTheDocument();
    expect(screen.getByText("orders-service has 2 direct callers")).toBeInTheDocument();
    expect(jsonLink()).toBe("/api/v1/scopes/webshop/PROD/k8s-prod-eu1/findings?severity=MEDIUM");

    await user.selectOptions(screen.getByRole("combobox", { name: "Rule" }), "unknown-external");
    expect(screen.getByText("No finding matches the filter.")).toBeInTheDocument();
    expect(jsonLink()).toBe(
      "/api/v1/scopes/webshop/PROD/k8s-prod-eu1/findings?severity=MEDIUM&rule=unknown-external",
    );

    await user.click(screen.getByRole("button", { name: /^All/ }));
    expect(screen.getByText("Unknown external system api.stripe.com")).toBeInTheDocument();
    expect(jsonLink()).toBe(
      "/api/v1/scopes/webshop/PROD/k8s-prod-eu1/findings?rule=unknown-external",
    );
  });

  it("expands a finding to its evidence, links to the map and offers the allowlist line", async () => {
    const user = userEvent.setup();
    renderAt(`${FINDINGS}?rule=unknown-external`, <App />);
    const title = await screen.findByText("Unknown external system api.stripe.com");

    await user.click(title);
    expect(screen.getByText("payments-service", { selector: "li" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Show on map" })).toHaveAttribute(
      "href",
      "/scopes/webshop/PROD/k8s-prod-eu1?node=external%3Aapi.stripe.com",
    );
    expect(
      screen.getByText("architrace.rules.unknown-external.allowlist: api.stripe.com"),
    ).toBeInTheDocument();
    expect(screen.getByRole("combobox", { name: "Rule" })).toHaveDisplayValue("Unknown external");
  });

  it("says so when there are no findings and reports a failing control plane", async () => {
    vi.stubGlobal(
      "fetch",
      respondByPath({ "/findings": [], "/graph": demoGraph, "/scopes": demoScopes }),
    );
    const { unmount } = renderAt(FINDINGS, <App />);
    expect(await screen.findByText(/No findings\./)).toBeInTheDocument();
    expect(rail().getAllByText("pass")).toHaveLength(7);
    unmount();

    vi.stubGlobal("fetch", respondWithJson(problem, 404, "application/problem+json"));
    renderAt(FINDINGS, <App />);
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The control plane did not answer: no agent has registered for scope",
    );
  });
});
