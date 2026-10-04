/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.core.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.google.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class AgentConfigLoader {

    private final ObjectMapper yaml;

    @Inject
    public AgentConfigLoader() {
        this.yaml = new ObjectMapper(YAMLFactory.builder()
                .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
                .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
                .build());
    }

    public AgentConfig load(Path configPath, Map<String, String> overrides) {
        Objects.requireNonNull(configPath, "configPath");
        ObjectNode root = readTree(configPath);
        PropertyOverrides.apply(root, overrides, yaml);
        AgentConfigDocument document = toDocument(root);
        List<String> problems = document.problems();
        if (!problems.isEmpty()) {
            throw new AgentConfigException(problems);
        }
        return document.toConfig();
    }

    public String render(AgentConfig config) {
        try {
            return yaml.writeValueAsString(AgentConfigDocument.of(config));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to render the configuration", e);
        }
    }

    private ObjectNode readTree(Path configPath) {
        if (!Files.isRegularFile(configPath)) {
            throw new AgentConfigException("Config file does not exist: " + configPath);
        }
        try (InputStream in = Files.newInputStream(configPath)) {
            JsonNode tree = yaml.readTree(in);
            if (tree == null || tree.isMissingNode() || tree.isNull()) {
                return yaml.createObjectNode();
            }
            if (tree instanceof ObjectNode object) {
                return object;
            }
            throw new AgentConfigException("Config must be a YAML mapping: " + configPath);
        } catch (JsonProcessingException e) {
            throw new AgentConfigException("Config file is not valid YAML: " + e.getOriginalMessage());
        } catch (IOException e) {
            throw new AgentConfigException("Failed to read config file " + configPath + ": " + e.getMessage());
        }
    }

    private AgentConfigDocument toDocument(ObjectNode root) {
        try {
            return yaml.treeToValue(root, AgentConfigDocument.class);
        } catch (UnrecognizedPropertyException e) {
            throw new AgentConfigException("Unknown config field: " + path(e));
        } catch (JsonMappingException e) {
            throw new AgentConfigException("Invalid config field: " + path(e) + " (" + e.getOriginalMessage() + ")");
        } catch (JsonProcessingException e) {
            throw new AgentConfigException("Invalid config: " + e.getOriginalMessage());
        }
    }

    private static String path(JsonMappingException exception) {
        return exception.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("."));
    }
}
