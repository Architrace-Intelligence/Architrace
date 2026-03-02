/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.grpc;


import java.net.InetSocketAddress;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GrpcAddressParserTest {

  @Test
  void parseHostPortShouldParseValidAddress() {
    InetSocketAddress address = GrpcAddressParser.parseHostPort("localhost:50051");

    assertThat(address.getHostString()).isEqualTo("localhost");
    assertThat(address.getPort()).isEqualTo(50051);
  }

  @Test
  void parseHostPortShouldFailForMissingPort() {
    assertThatThrownBy(() -> GrpcAddressParser.parseHostPort("localhost"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Expected host:port");
  }

  @Test
  void parseHostPortShouldFailForTrailingSeparator() {
    assertThatThrownBy(() -> GrpcAddressParser.parseHostPort("localhost:"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Expected host:port");
  }

  @Test
  void parseHostPortShouldFailForNonNumericPort() {
    assertThatThrownBy(() -> GrpcAddressParser.parseHostPort("localhost:abc"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Port must be numeric.");
  }
}
