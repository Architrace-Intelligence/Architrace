/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.cli;


import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.architrace.testsupport.TestDataProvider;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DryRunCommandTest {

  @TempDir
  Path tempDir;

  @InjectMocks
  private DryRunCommand sut;

  @Test
  void runShouldValidateConfigSuccessfully() throws Exception {
    Path configPath =
        TestDataProvider.copyResourceToTemp(
            tempDir,
            "testdata/config/valid-agent-config.json",
            "valid-agent-config.json");
    setField(sut, "configPath", configPath.toString());

    assertThatCode(sut::run).doesNotThrowAnyException();
  }

  @Test
  void runShouldAcceptPropertiesOverridesWithoutFailing() throws Exception {
    Path configPath =
        TestDataProvider.copyResourceToTemp(
            tempDir,
            "testdata/config/valid-agent-config.json",
            "valid-agent-config.json");
    setField(sut, "configPath", configPath.toString());
    setField(sut, "properties", Map.of("feature.enabled", "true"));

    assertThatCode(sut::run).doesNotThrowAnyException();
  }

  private static void setField(Object target, String name, Object value) throws Exception {
    Field field = target.getClass().getDeclaredField(name);
    field.setAccessible(true);
    field.set(target, value);
  }
}
