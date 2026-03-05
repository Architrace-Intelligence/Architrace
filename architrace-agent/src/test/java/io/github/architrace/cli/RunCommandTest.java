/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.cli;


import io.github.architrace.core.config.AgentConfigLoader;
import io.github.architrace.service.runtime.AgentRuntimeService;
import java.io.File;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RunCommandTest {

  @Test
  void runShouldFailFastWhenConfigFileDoesNotExist() throws Exception {
    RunCommand sut = new RunCommand(new AgentRuntimeService(new AgentConfigLoader(), null));
    File missing = new File("build/this-config-does-not-exist.yaml");
    setField(sut, "configFile", missing);

    assertThatThrownBy(sut::run)
        .isInstanceOf(IllegalStateException.class)
        .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
