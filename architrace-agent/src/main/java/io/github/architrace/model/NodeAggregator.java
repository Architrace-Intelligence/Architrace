/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NodeAggregator {

  public Map<LogicalServiceId, LogicalServiceNode> aggregate(List<InternalSpan> spans) {
    return new HashMap<>();
  }
}