/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

import { queryOptions } from "@tanstack/react-query";
import { listScopes } from "./client";

export function scopesQuery() {
  return queryOptions({
    queryKey: ["scopes"],
    queryFn: ({ signal }) => listScopes(signal),
  });
}
