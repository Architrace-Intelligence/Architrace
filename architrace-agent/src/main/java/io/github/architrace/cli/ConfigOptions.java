/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.cli;

import io.github.architrace.core.config.AgentConfig;
import io.github.architrace.core.config.AgentConfigException;
import io.github.architrace.core.config.AgentConfigLoader;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public final class ConfigOptions {

    @Option(
            names = "--config",
            required = true,
            paramLabel = "<path>",
            description = "Path to the YAML configuration file")
    private Path configPath;

    @Option(
            names = "--prop",
            paramLabel = "<key=value>",
            description = "Override a configuration value by its dotted path, for example otlp.port=4320")
    private Map<String, String> overrides = new LinkedHashMap<>();

    AgentConfig load(AgentConfigLoader loader) {
        return loader.load(configPath, overrides);
    }

    static int reportInvalid(AgentConfigException exception, PrintWriter err) {
        err.println("Configuration is invalid:");
        exception.problems().forEach(problem -> err.println("  - " + problem));
        err.flush();
        return CommandLine.ExitCode.SOFTWARE;
    }
}
