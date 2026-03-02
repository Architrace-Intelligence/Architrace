/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.grpc;

import java.net.InetSocketAddress;

public final class GrpcAddressParser {

  private GrpcAddressParser() {
  }

  public static InetSocketAddress parseHostPort(String server) {
    int separatorIndex = server.lastIndexOf(':');
    if (separatorIndex <= 0 || separatorIndex == server.length() - 1) {
      throw new IllegalArgumentException(
          "Invalid control-plane.bootstrap.server value '" + server + "'. Expected host:port");
    }

    String host = server.substring(0, separatorIndex);
    int port;
    try {
      port = Integer.parseInt(server.substring(separatorIndex + 1));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          "Invalid control-plane.bootstrap.server value '" + server + "'. Port must be numeric.",
          e);
    }
    return new InetSocketAddress(host, port);
  }
}
