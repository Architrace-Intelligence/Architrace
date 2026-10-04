/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { renderAt } from "../test/render";
import { Shell } from "./Shell";

describe("Shell", () => {
  afterEach(() => {
    window.localStorage.clear();
    delete document.documentElement.dataset.theme;
    vi.restoreAllMocks();
  });

  it("toggles and remembers the theme", async () => {
    const user = userEvent.setup();
    renderAt(
      "/",
      <Shell title="Projects" apiRequest="/api/v1/scopes">
        content
      </Shell>,
    );

    expect(document.documentElement.dataset.theme).toBe("dark");
    await user.click(screen.getByRole("button", { name: "Switch to the light theme" }));

    expect(document.documentElement.dataset.theme).toBe("light");
    expect(window.localStorage.getItem("architrace.theme")).toBe("light");
    expect(screen.getByRole("button", { name: "Switch to the dark theme" })).toBeInTheDocument();
  });

  it("starts from the stored theme and survives a broken storage", () => {
    window.localStorage.setItem("architrace.theme", "light");
    const { unmount } = renderAt(
      "/",
      <Shell title="Projects" apiRequest="/api/v1/scopes">
        content
      </Shell>,
    );
    expect(document.documentElement.dataset.theme).toBe("light");
    unmount();

    vi.spyOn(Storage.prototype, "getItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new Error("blocked");
    });
    renderAt(
      "/",
      <Shell title="Projects" apiRequest="/api/v1/scopes">
        content
      </Shell>,
    );

    expect(document.documentElement.dataset.theme).toBe("dark");
  });

  it("disables the map and drift entries until a scope is open", () => {
    const { unmount } = renderAt(
      "/",
      <Shell title="Projects" apiRequest="/api/v1/scopes">
        content
      </Shell>,
    );
    expect(screen.getByText("Map")).toHaveAttribute("aria-disabled", "true");
    expect(screen.getByText("Drift")).toHaveAttribute("aria-disabled", "true");
    expect(screen.getByRole("link", { name: "Projects" })).toHaveAttribute("aria-current", "page");
    unmount();

    renderAt(
      "/scopes/webshop/PROD/k8s-prod-eu1",
      <Shell
        title="Service map"
        apiRequest="/api/v1/scopes"
        scopePath="/scopes/webshop/PROD/k8s-prod-eu1"
      >
        content
      </Shell>,
    );
    expect(screen.getByRole("link", { name: "Map" })).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("link", { name: "Drift" })).toHaveAttribute(
      "href",
      "/scopes/webshop/PROD/k8s-prod-eu1/drift",
    );
    expect(screen.getByRole("link", { name: "Drift" })).not.toHaveAttribute("aria-current");
    expect(screen.getByRole("link", { name: "Projects" })).not.toHaveAttribute("aria-current");
  });

  it("copies the link of the current view and says so for a moment", async () => {
    const user = userEvent.setup();
    renderAt(
      "/scopes/webshop/PROD/k8s-prod-eu1?node=a",
      <Shell title="Service map" apiRequest="/api/v1/scopes" aside={<p>rail</p>}>
        content
      </Shell>,
    );
    expect(screen.getByRole("complementary", { name: "Context" })).toHaveTextContent("rail");

    await user.click(screen.getByRole("button", { name: "Copy link" }));

    expect(await screen.findByRole("button", { name: "Copied" })).toBeInTheDocument();
    await expect(navigator.clipboard.readText()).resolves.toBe(window.location.href);
    await waitFor(
      () => {
        expect(screen.getByRole("button", { name: "Copy link" })).toBeInTheDocument();
      },
      { timeout: 3_000 },
    );
  });
});
