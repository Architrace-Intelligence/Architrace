/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace;


import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MainAppTest {

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
