/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

const SECOND = 1_000;
const MINUTE = 60 * SECOND;
const HOUR = 60 * MINUTE;
const DAY = 24 * HOUR;

export function formatRelative(iso: string | undefined, now: number): string {
  if (iso === undefined) {
    return "never";
  }
  const elapsed = Math.max(0, now - Date.parse(iso));
  if (elapsed < MINUTE) {
    return `${String(Math.round(elapsed / SECOND))} s ago`;
  }
  if (elapsed < HOUR) {
    return `${String(Math.round(elapsed / MINUTE))} min ago`;
  }
  if (elapsed < DAY) {
    return `${String(Math.round(elapsed / HOUR))} h ago`;
  }
  return `${String(Math.round(elapsed / DAY))} d ago`;
}
