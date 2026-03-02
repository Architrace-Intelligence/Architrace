/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.config;

import io.github.architrace.grpc.GrpcAddressParser;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

public final class ControlPlaneTransportConfig {

  private ControlPlaneTransportConfig() {
  }

  public static ManagedChannel createChannel(String server) {
    var address = GrpcAddressParser.parseHostPort(server);
    return NettyChannelBuilder.forAddress(address).usePlaintext().build();
  }
}

