/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.testsupport;

import io.github.architrace.grpc.proto.ConfigUpdate;
import java.io.IOException;
import java.util.Map;

public final class GrpcEventDataFactory {

  private GrpcEventDataFactory() {
  }

  @SuppressWarnings("unchecked")
  public static ConfigUpdate createConfigUpdateFromResource(String resourcePath) throws IOException {
    Map<String, Object> payload = TestDataProvider.readJsonObject(resourcePath);
    String version = (String) payload.get("version");
    Map<String, String> entries = (Map<String, String>) payload.get("entries");
    return ConfigUpdate.newBuilder().setVersion(version).putAllConfig(entries).build();
  }
}
