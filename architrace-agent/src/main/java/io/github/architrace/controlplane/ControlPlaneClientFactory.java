/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.controlplane;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.architrace.config.ControlPlaneTransportConfig;
import io.github.architrace.grpc.ControlPlaneClient;
import io.github.architrace.inbound.ControlMessageHandler;
import java.util.List;
import java.util.Set;

@Singleton
public class ControlPlaneClientFactory {

  private final List<ControlMessageHandler> handlers;
  private final RegistrationService registrationService;

  @Inject
  public ControlPlaneClientFactory(
      Set<ControlMessageHandler> handlers,
      RegistrationService registrationService) {
    this.handlers = List.copyOf(handlers);
    this.registrationService = registrationService;
  }

  public ControlPlaneLifecycle create(String server, String agentName) {
    var channel = ControlPlaneTransportConfig.createChannel(server);
    var transportClient = new ControlPlaneClient(channel);

    return new ControlPlaneLifecycle(agentName, transportClient, registrationService, handlers);
  }
}
