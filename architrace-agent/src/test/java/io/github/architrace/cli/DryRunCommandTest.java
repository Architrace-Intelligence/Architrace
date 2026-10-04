/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.cli;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.core.config.AgentConfigLoader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class DryRunCommandTest {

    @TempDir
    Path tempDir;

    private final StringWriter out = new StringWriter();
    private final StringWriter err = new StringWriter();
    private final CommandLine commandLine = new CommandLine(new DryRunCommand(new AgentConfigLoader()))
            .setOut(new PrintWriter(out))
            .setErr(new PrintWriter(err));

    @Test
    void printsTheEffectiveConfigurationWithOverridesApplied() throws IOException {
        Path config = Files.writeString(tempDir.resolve("agent.yaml"), """
                environment: DEV
                cluster: local
                agent:
                  name: dev-agent
                control-plane:
                  server: localhost:9090
                """);

        int exitCode = commandLine.execute("--config", config.toString(), "--prop", "otlp.port=4320");

        assertThat(exitCode).isEqualTo(CommandLine.ExitCode.OK);
        assertThat(out.toString())
                .contains("project: default\n")
                .contains("otlp:\n  port: 4320\n")
                .contains("snapshot:\n  interval-seconds: 60\n")
                .contains("- deployment.environment.name\n");
        assertThat(err.toString()).isEmpty();
    }

    @Test
    void listsEveryProblemOfAnInvalidConfiguration() throws IOException {
        Path config = Files.writeString(tempDir.resolve("agent.yaml"), "environment: DEV\n");

        int exitCode = commandLine.execute("--config", config.toString());

        assertThat(exitCode).isEqualTo(CommandLine.ExitCode.SOFTWARE);
        assertThat(out.toString()).isEmpty();
        assertThat(err.toString())
                .startsWith("Configuration is invalid:\n")
                .contains("  - Missing required config field: cluster\n")
                .contains("  - Missing required config field: agent.name\n")
                .contains("  - Missing required config field: control-plane.server\n");
    }

    @Test
    void requiresTheConfigOption() {
        int exitCode = commandLine.execute();

        assertThat(exitCode).isEqualTo(CommandLine.ExitCode.USAGE);
        assertThat(err.toString()).contains("--config");
    }
}
