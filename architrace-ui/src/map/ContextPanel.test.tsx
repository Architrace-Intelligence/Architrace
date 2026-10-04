/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { demoGraph } from "../test/http";
import { ContextPanel } from "./ContextPanel";
import { INITIAL_MAP_STATE, type Selection } from "./model";

function renderPanel(selection: Selection | undefined) {
  const onSelect = vi.fn();
  render(
    <ContextPanel
      graph={demoGraph}
      state={{ ...INITIAL_MAP_STATE, selection }}
      onSelect={onSelect}
    />,
  );
  return onSelect;
}

function value(term: string): HTMLElement {
  const definition = screen.getByText(term).nextElementSibling;
  if (!(definition instanceof HTMLElement)) {
    throw new Error(`${term} has no value`);
  }
  return definition;
}

describe("ContextPanel", () => {
  it("summarises the scope and opens a data stream", async () => {
    const user = userEvent.setup();
    const onSelect = renderPanel(undefined);

    expect(screen.getByText("Scope · PROD")).toBeInTheDocument();
    expect(screen.getByText("webshop")).toBeInTheDocument();
    expect(value("Services")).toHaveTextContent("5 in 5 namespaces");
    expect(value("Dependencies")).toHaveTextContent("11 · 7 sync, 4 stream");
    expect(value("Graph at")).toHaveTextContent("2026-10-01 12:00:00 UTC");
    const streams = screen.getByRole("region", { name: "Data streams" });
    expect(within(streams).getByText("orders-service → notification-service")).toBeInTheDocument();

    await user.click(within(streams).getByRole("button", { name: /order-events/ }));

    expect(onSelect).toHaveBeenCalledWith({ kind: "node", id: "topic:kafka/order-events" });
  });

  it("describes a service with its streams and dependencies and selects a dependency", async () => {
    const user = userEvent.setup();
    const onSelect = renderPanel({ kind: "node", id: "service:orders-service" });

    expect(screen.getByText("Service · PROD")).toBeInTheDocument();
    expect(screen.getByText("namespace orders")).toBeInTheDocument();
    expect(screen.getByText("v2.8.1")).toBeInTheDocument();
    expect(value("Clusters")).toHaveTextContent("k8s-prod-eu1");
    expect(value("team")).toHaveTextContent("orders");
    expect(value("Inbound")).toHaveTextContent("2 dependencies · 13.6k calls");
    expect(value("Outbound")).toHaveTextContent("3 dependencies · 41.6k calls");
    const streams = screen.getByRole("region", { name: "Data streams" });
    expect(
      within(streams).getByRole("button", { name: "publish order-events 8.7k · 0.0 %" }),
    ).toBeInTheDocument();
    const dependencies = screen.getByRole("region", { name: "Dependencies · 2 in, 2 out" });

    await user.click(
      within(dependencies).getByRole("button", { name: "in api-gateway 12.4k · 0.3 %" }),
    );
    expect(onSelect).toHaveBeenCalledWith({
      kind: "edge",
      id: "service:api-gateway>service:orders-service:SYNC",
    });

    await user.click(screen.getByRole("button", { name: "Close details" }));
    expect(onSelect).toHaveBeenCalledWith(undefined);
  });

  it("says when a service touches no stream", () => {
    renderPanel({ kind: "node", id: "service:api-gateway" });

    expect(screen.getByText("Neither publishes nor consumes a data stream.")).toBeInTheDocument();
    expect(screen.getByText("namespace edge")).toBeInTheDocument();
  });

  it("lists the callers of a data store without a streams section", () => {
    renderPanel({ kind: "node", id: "db:postgresql/orders" });

    expect(screen.getByText("Data store · PROD")).toBeInTheDocument();
    expect(
      within(screen.getByRole("region", { name: "Dependencies · 1 in, 0 out" })).getByRole(
        "button",
        { name: "in orders-service 24.0k · 0.0 %" },
      ),
    ).toBeInTheDocument();
    expect(screen.queryByRole("region", { name: "Data streams" })).not.toBeInTheDocument();
  });

  it("describes a topic with its producers and consumers", () => {
    renderPanel({ kind: "node", id: "topic:kafka/order-events" });

    expect(screen.getByText("Data stream · PROD")).toBeInTheDocument();
    expect(
      within(screen.getByRole("region", { name: "Producers · 1" })).getByRole("button", {
        name: "publish orders-service 8.7k · 0.0 %",
      }),
    ).toBeInTheDocument();
    expect(
      within(screen.getByRole("region", { name: "Consumers · 1" })).getByRole("button", {
        name: "consume notification-service 8.7k · 0.0 %",
      }),
    ).toBeInTheDocument();
    expect(screen.queryByText(/Dependencies ·/)).not.toBeInTheDocument();
  });

  it("describes a dependency and selects its ends", async () => {
    const user = userEvent.setup();
    const onSelect = renderPanel({
      kind: "edge",
      id: "service:payments-service>external:api.stripe.com:SYNC",
    });

    expect(screen.getByText("Dependency · PROD")).toBeInTheDocument();
    expect(screen.getByText("payments-service → api.stripe.com")).toBeInTheDocument();
    expect(screen.getByText("errors ≥ 3 %")).toBeInTheDocument();
    expect(value("Calls")).toHaveTextContent("3000");
    expect(value("Errors")).toHaveTextContent("150 · 5.0 %");
    expect(value("p50 / p95")).toHaveTextContent("4 ms / 20 ms");
    expect(value("p99 / max")).toHaveTextContent("60 ms / 300 ms");

    await user.click(screen.getByRole("button", { name: "to api.stripe.com External" }));

    expect(onSelect).toHaveBeenCalledWith({ kind: "node", id: "external:api.stripe.com" });
  });

  it("falls back to the scope when the selection is unknown", () => {
    renderPanel({ kind: "edge", id: "a>b:SYNC" });
    expect(screen.getByText("Scope · PROD")).toBeInTheDocument();

    renderPanel({ kind: "node", id: "service:ghost" });
    expect(screen.getAllByText("Scope · PROD")).toHaveLength(2);
  });
});
