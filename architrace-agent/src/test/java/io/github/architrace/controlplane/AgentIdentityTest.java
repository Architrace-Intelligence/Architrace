/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.controlplane;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.architrace.core.BuildVersion;
import io.github.architrace.testsupport.TestDataProvider;
import org.junit.jupiter.api.Test;

class AgentIdentityTest {

    @Test
    void identityIsTakenFromTheConfigurationAndTheBuildVersion() {
        AgentIdentity identity = AgentIdentity.from(TestDataProvider.agentConfig());

        assertThat(identity)
                .isEqualTo(new AgentIdentity("agent-a", BuildVersion.current(), "demo", "PROD", "cluster-1"));
    }

    @Test
    void identityRejectsMissingComponents() {
        assertThatNullPointerException()
                .isThrownBy(() -> new AgentIdentity("a", "1", "p", null, "c"))
                .withMessage("environment");
    }
}
