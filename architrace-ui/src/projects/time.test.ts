/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { describe, expect, it } from "vitest";
import { formatRelative } from "./time";

describe("formatRelative", () => {
  const now = Date.parse("2026-10-01T12:00:00Z");

  it("describes the elapsed time in the largest sensible unit", () => {
    expect(formatRelative(undefined, now)).toBe("never");
    expect(formatRelative("2026-10-01T11:59:02Z", now)).toBe("58 s ago");
    expect(formatRelative("2026-10-01T11:45:00Z", now)).toBe("15 min ago");
    expect(formatRelative("2026-10-01T10:00:00Z", now)).toBe("2 h ago");
    expect(formatRelative("2026-09-28T12:00:00Z", now)).toBe("3 d ago");
  });

  it("never reports the future as negative", () => {
    expect(formatRelative("2026-10-01T12:00:30Z", now)).toBe("0 s ago");
  });
});
