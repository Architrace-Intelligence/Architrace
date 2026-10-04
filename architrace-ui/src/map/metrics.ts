/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import type { EdgeMetrics } from "../api/client";
import { errorRate } from "./model";

export function formatCount(value: number): string {
  if (value >= 1_000_000) {
    return `${(value / 1_000_000).toFixed(1)}M`;
  }
  return value >= 1_000 ? `${(value / 1_000).toFixed(1)}k` : String(value);
}

export function formatRate(metrics: EdgeMetrics): string {
  return `${(errorRate(metrics) * 100).toFixed(1)} %`;
}

export function formatMillis(millis: number): string {
  return millis >= 1_000 ? `${(millis / 1_000).toFixed(1)} s` : `${String(millis)} ms`;
}

export function describeMetrics(metrics: EdgeMetrics): string {
  return `${formatCount(metrics.calls)} · ${formatRate(metrics)}`;
}

export function formatInstant(iso: string): string {
  const millis = Date.parse(iso);
  if (Number.isNaN(millis)) {
    return iso;
  }
  return `${new Date(millis).toISOString().slice(0, 19).replace("T", " ")} UTC`;
}
