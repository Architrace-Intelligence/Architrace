/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(OpenApiDocumentController.class)
class OpenApiDocumentControllerTest {

  @Autowired MockMvcTester mvc;

  @Test
  void servesTheContractDocumentAsYaml() {
    assertThat(mvc.get().uri("/api/v1/openapi.yaml"))
        .hasStatusOk()
        .hasContentTypeCompatibleWith(MediaType.parseMediaType("application/yaml"))
        .bodyText()
        .contains("openapi: 3.1.0")
        .contains("/scopes:")
        .contains("/agents:");
  }
}
