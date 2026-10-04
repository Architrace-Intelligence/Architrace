/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { demoGraph } from "../test/http";
import { renderAt } from "../test/render";
import { INITIAL_MAP_STATE } from "./model";
import { ServiceMap } from "./ServiceMap";

vi.mock("./layout", async (importOriginal) => ({
  ...(await importOriginal<typeof import("./layout")>()),
  layoutGraph: vi.fn(() => Promise.reject(new Error("the layout engine did not load"))),
}));

describe("ServiceMap", () => {
  it("reports while the layout runs and when it fails", async () => {
    renderAt(
      "/",
      <ServiceMap graph={demoGraph} state={INITIAL_MAP_STATE} onSelect={() => undefined} />,
    );

    expect(screen.getByRole("status")).toHaveTextContent("Laying out 10 nodes…");
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "The layout failed: the layout engine did not load",
    );
  });
});
