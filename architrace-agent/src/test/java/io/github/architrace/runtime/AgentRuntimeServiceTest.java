/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.runtime;


import io.github.architrace.core.config.AgentConfigLoader;
import java.nio.file.Path;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentRuntimeServiceTest {

  @TempDir
  Path tempDir;

  @Test
  void runShouldFailFastWhenConfigDoesNotExist() {
    AgentRuntimeService sut = new AgentRuntimeService(new AgentConfigLoader(), null);
    Path missingConfig = tempDir.resolve("missing-config.yaml");

    assertThatThrownBy(() -> sut.run(missingConfig))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
