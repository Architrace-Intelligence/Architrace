/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { problem, respondWithJson, scopeSummary } from "../test/http";
import { App } from "./App";

function renderApp() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return render(
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>,
  );
}

describe("App", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("shows the scopes the control plane reports", async () => {
    vi.stubGlobal(
      "fetch",
      respondWithJson([
        scopeSummary("webshop", "PROD", "k8s-prod-eu1"),
        scopeSummary("webshop", "DEV", "k8s-dev"),
      ]),
    );

    renderApp();

    expect(screen.getByRole("heading", { name: "Projects" })).toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("Connecting to the control plane…");
    expect(await screen.findByText("2 scopes reported by agents.")).toBeInTheDocument();
  });

  it("counts a single scope in the singular", async () => {
    vi.stubGlobal("fetch", respondWithJson([scopeSummary("webshop", "PROD", "k8s-prod-eu1")]));

    renderApp();

    expect(await screen.findByText("1 scope reported by agents.")).toBeInTheDocument();
  });

  it("shows the problem the control plane answered", async () => {
    vi.stubGlobal("fetch", respondWithJson(problem, 404, "application/problem+json"));

    renderApp();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The control plane did not answer: no agent has registered for scope webshop/PROD/k8s-prod-eu2",
    );
  });

  it("falls back to the problem title when there is no detail", async () => {
    vi.stubGlobal(
      "fetch",
      respondWithJson(
        { title: "Service Unavailable", status: 503 },
        503,
        "application/problem+json",
      ),
    );

    renderApp();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The control plane did not answer: Service Unavailable",
    );
  });

  it("shows a failure to reach the control plane", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() => Promise.reject(new TypeError("Failed to fetch"))),
    );

    renderApp();

    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The control plane did not answer: Failed to fetch",
    );
  });
});
