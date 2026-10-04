/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.core.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

final class PropertyOverrides {

    private PropertyOverrides() {}

    static void apply(ObjectNode root, Map<String, String> overrides, ObjectMapper yaml) {
        overrides.forEach((key, value) -> set(root, key, value, yaml));
    }

    private static void set(ObjectNode root, String key, String value, ObjectMapper yaml) {
        List<String> path = Arrays.asList(key.split("\\.", -1));
        if (path.stream().anyMatch(String::isBlank)) {
            throw new AgentConfigException("Invalid override key: '" + key + "'");
        }
        ObjectNode parent = descend(root, path.subList(0, path.size() - 1));
        parent.set(path.getLast(), parse(key, value, yaml));
    }

    private static ObjectNode descend(ObjectNode node, List<String> path) {
        if (path.isEmpty()) {
            return node;
        }
        String name = path.getFirst();
        ObjectNode child = node.get(name) instanceof ObjectNode existing ? existing : node.putObject(name);
        return descend(child, path.subList(1, path.size()));
    }

    private static JsonNode parse(String key, String value, ObjectMapper yaml) {
        try {
            JsonNode node = yaml.readTree(value);
            return node == null || node.isMissingNode() ? NullNode.getInstance() : node;
        } catch (JsonProcessingException e) {
            throw new AgentConfigException("Invalid override value for '" + key + "': " + e.getOriginalMessage());
        }
    }
}
