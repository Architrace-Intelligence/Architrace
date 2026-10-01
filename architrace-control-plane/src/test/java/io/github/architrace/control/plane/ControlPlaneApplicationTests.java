/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest(properties = "spring.grpc.server.enabled=false")
@AutoConfigureMockMvc
@Import(PostgresTestcontainers.class)
class ControlPlaneApplicationTests {

  @Autowired JdbcClient jdbc;
  @Autowired MockMvcTester mvc;

  @Test
  void appliesSchemaOnStartup() {
    Integer changeSets =
        jdbc.sql("select count(*) from databasechangelog").query(Integer.class).single();
    assertThat(changeSets).isGreaterThanOrEqualTo(4);
  }

  @Test
  void servesSwaggerUiOnTopOfTheContractDocument() {
    assertThat(mvc.get().uri("/swagger-ui"))
        .hasStatus3xxRedirection()
        .hasRedirectedUrl("/swagger-ui/index.html");
    assertThat(mvc.get().uri("/swagger-ui/index.html"))
        .hasStatusOk()
        .bodyText()
        .contains("/api/v1/openapi.yaml");
    assertThat(mvc.get().uri("/webjars/swagger-ui/swagger-ui-bundle.js")).hasStatusOk();
    assertThat(mvc.get().uri("/v3/api-docs")).hasStatus(404);
  }
}
