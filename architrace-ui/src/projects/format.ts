/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

export function plural(count: number, noun: string, nouns = `${noun}s`): string {
  return `${String(count)} ${count === 1 ? noun : nouns}`;
}
