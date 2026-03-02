/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.runtime;

import io.github.architrace.core.config.AgentConfig;

public record RuntimeConfigSummary(
    String clusterId,
    String agentName,
    String controlPlaneServer) {
}
