/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.architrace.core.config.AgentConfig;
import io.github.architrace.core.config.AgentConfigLoader;
import io.github.architrace.service.runtime.AgentRuntimeService;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import picocli.CommandLine;

class RunCommandTest {

    @TempDir
    Path tempDir;

    private final AgentRuntimeService runtime = mock(AgentRuntimeService.class);
    private final StringWriter err = new StringWriter();
    private final CommandLine commandLine = new CommandLine(new RunCommand(new AgentConfigLoader(), runtime))
            .setOut(new PrintWriter(new StringWriter()))
            .setErr(new PrintWriter(err));

    @Test
    void startsTheRuntimeWithTheLoadedConfiguration() throws Exception {
        Path config = validConfig();

        int exitCode = commandLine.execute("--config", config.toString(), "--prop", "project=webshop");

        assertThat(exitCode).isEqualTo(CommandLine.ExitCode.OK);
        ArgumentCaptor<AgentConfig> captor = ArgumentCaptor.forClass(AgentConfig.class);
        verify(runtime).run(captor.capture());
        assertThat(captor.getValue().project()).isEqualTo("webshop");
        assertThat(captor.getValue().agentName()).isEqualTo("dev-agent");
    }

    @Test
    void failsFastWhenTheConfigurationIsInvalid() {
        Path missing = tempDir.resolve("missing.yaml");

        int exitCode = commandLine.execute("--config", missing.toString());

        assertThat(exitCode).isEqualTo(CommandLine.ExitCode.SOFTWARE);
        assertThat(err.toString()).contains("Config file does not exist: " + missing);
        verifyNoInteractions(runtime);
    }

    @Test
    void reportsARuntimeFailureThroughTheExitCode() throws Exception {
        Path config = validConfig();
        doThrow(new InterruptedException("stopped")).when(runtime).run(any());

        int exitCode = commandLine.execute("--config", config.toString());

        assertThat(exitCode).isEqualTo(CommandLine.ExitCode.SOFTWARE);
        assertThat(err.toString()).contains("stopped");
    }

    private Path validConfig() throws IOException {
        return Files.writeString(tempDir.resolve("agent.yaml"), """
                environment: DEV
                cluster: local
                agent:
                  name: dev-agent
                control-plane:
                  server: localhost:9090
                """);
    }
}
