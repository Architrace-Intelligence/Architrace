/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.controlplane;

import com.google.inject.Inject;
import io.github.architrace.core.config.AgentConfig;

public class ControlPlaneBootstrapService {

  private final ControlPlaneClientFactory controlPlaneClientFactory;

  @Inject
  public ControlPlaneBootstrapService(ControlPlaneClientFactory controlPlaneClientFactory) {
    this.controlPlaneClientFactory = controlPlaneClientFactory;
  }

  public ControlPlaneLifecycle bootstrap(AgentConfig config) {
    var agentName = config.agent().name();
    var controlPlaneServer = config.controlPlane().bootstrap().server();

    return controlPlaneClientFactory.create(controlPlaneServer, agentName);
  }
}
