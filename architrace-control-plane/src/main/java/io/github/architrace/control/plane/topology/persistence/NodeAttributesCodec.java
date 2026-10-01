/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.persistence;

import io.github.architrace.control.plane.topology.NodeAttributes;
import tools.jackson.databind.json.JsonMapper;

final class NodeAttributesCodec {

  private final JsonMapper mapper = JsonMapper.builder().build();

  JsonDocument encode(NodeAttributes attributes) {
    return new JsonDocument(mapper.writeValueAsString(attributes));
  }

  NodeAttributes decode(JsonDocument document) {
    return mapper.readValue(document.json(), NodeAttributes.class);
  }
}
