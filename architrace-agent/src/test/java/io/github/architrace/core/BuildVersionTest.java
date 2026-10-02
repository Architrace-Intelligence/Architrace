/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BuildVersionTest {

  @Test
  void fallsBackToTheDevelopmentVersionWithoutAManifest() {
    assertThat(BuildVersion.current()).isEqualTo(BuildVersion.DEVELOPMENT);
    assertThat(BuildVersion.describe()).isEqualTo("Architrace dev");
  }
}
