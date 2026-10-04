/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

class MainAppTest {

  @TempDir
  Path tempDir;

  private static final int UNSET_EXIT_CODE = -1;

  @AfterEach
  void tearDown() {
    MainApp.resetExitHandler();
  }

  @Test
  void executeShouldReturnSuccessForVersionCommand() {
    int exitCode = MainApp.execute(new String[] {"version"});

    assertThat(exitCode).isEqualTo(CommandLine.ExitCode.OK);
  }

  @Test
  void executeShouldReturnSoftwareForUnknownOption() {
    int exitCode = MainApp.execute(new String[] {"--does-not-exist"});

    assertThat(exitCode).isEqualTo(CommandLine.ExitCode.SOFTWARE);
  }

  @Test
  void executeShouldWireDryRunThroughGuice() throws IOException {
    Path config = Files.writeString(
        tempDir.resolve("agent.yaml"),
        "environment: DEV\ncluster: local\nagent:\n  name: dev-agent\ncontrol-plane:\n  server: localhost:9090\n");

    assertThat(MainApp.execute(new String[] {"dry-run", "--config", config.toString()}))
        .isEqualTo(CommandLine.ExitCode.OK);
    assertThat(MainApp.execute(new String[] {"dry-run", "--config", config.toString(), "--prop", "otlp.port=0"}))
        .isEqualTo(CommandLine.ExitCode.SOFTWARE);
  }

  @Test
  void setExitHandlerShouldRejectNull() {
    assertThatThrownBy(() -> MainApp.setExitHandler(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void mainShouldUseConfiguredExitHandler() {
    AtomicInteger capturedCode = new AtomicInteger(UNSET_EXIT_CODE);
    MainApp.setExitHandler(capturedCode::set);

    MainApp.main(new String[] {"version"});

    assertThat(capturedCode.get()).isEqualTo(CommandLine.ExitCode.OK);
  }
}
