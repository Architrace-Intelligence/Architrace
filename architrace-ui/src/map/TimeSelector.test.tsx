/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { fireEvent, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { TimeSelector } from "./TimeSelector";

describe("TimeSelector", () => {
  it("applies a point in time entered in UTC", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<TimeSelector at={undefined} onChange={onChange} />);

    await user.click(screen.getByRole("button", { name: "Live" }));
    const form = screen.getByRole("form", { name: "Point in time" });
    expect(within(form).getByRole("button", { name: "Apply" })).toBeDisabled();

    fireEvent.change(within(form).getByLabelText("Graph at (UTC)"), {
      target: { value: "2026-10-01T12:00:00" },
    });
    await user.click(within(form).getByRole("button", { name: "Apply" }));

    expect(onChange).toHaveBeenCalledWith("2026-10-01T12:00:00.000Z");
    expect(screen.queryByRole("form", { name: "Point in time" })).not.toBeInTheDocument();
  });

  it("shows the chosen time, prefills the field and goes back to live", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<TimeSelector at="2026-10-01T14:00:00+02:00" onChange={onChange} />);

    await user.click(screen.getByRole("button", { name: "At 2026-10-01 12:00:00 UTC" }));
    const form = screen.getByRole("form", { name: "Point in time" });
    expect(within(form).getByLabelText("Graph at (UTC)")).toHaveValue("2026-10-01T12:00");

    await user.click(within(form).getByRole("button", { name: "Live" }));

    expect(onChange).toHaveBeenCalledWith(undefined);
    expect(screen.queryByRole("form", { name: "Point in time" })).not.toBeInTheDocument();
  });

  it("closes on Escape without changing anything", async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<TimeSelector at={undefined} onChange={onChange} />);

    await user.click(screen.getByRole("button", { name: "Live" }));
    await user.keyboard("{Escape}");

    expect(screen.queryByRole("form", { name: "Point in time" })).not.toBeInTheDocument();
    expect(onChange).not.toHaveBeenCalled();
  });
});
