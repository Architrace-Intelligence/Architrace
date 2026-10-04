/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { describe, expect, it } from "vitest";
import { describeMetrics, formatCount, formatInstant, formatMillis, formatRate } from "./metrics";

const metrics = {
  calls: 12_400,
  errors: 37,
  p50Millis: 12,
  p95Millis: 48,
  p99Millis: 90,
  maxMillis: 1_500,
};

describe("metrics formatting", () => {
  it("abbreviates counts in thousands and millions", () => {
    expect(formatCount(0)).toBe("0");
    expect(formatCount(950)).toBe("950");
    expect(formatCount(1_200)).toBe("1.2k");
    expect(formatCount(24_000)).toBe("24.0k");
    expect(formatCount(1_260_000)).toBe("1.3M");
  });

  it("formats the error rate, latencies and the one-line summary", () => {
    expect(formatRate(metrics)).toBe("0.3 %");
    expect(formatRate({ ...metrics, calls: 0, errors: 0 })).toBe("0.0 %");
    expect(formatMillis(12)).toBe("12 ms");
    expect(formatMillis(1_500)).toBe("1.5 s");
    expect(describeMetrics(metrics)).toBe("12.4k · 0.3 %");
  });

  it("formats an instant in UTC and leaves garbage alone", () => {
    expect(formatInstant("2026-10-01T12:00:00Z")).toBe("2026-10-01 12:00:00 UTC");
    expect(formatInstant("2026-10-01T14:00:00+02:00")).toBe("2026-10-01 12:00:00 UTC");
    expect(formatInstant("yesterday")).toBe("yesterday");
  });
});
