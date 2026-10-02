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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@SpringBootTest(properties = "spring.grpc.server.enabled=false")
@AutoConfigureMockMvc
@Import(PostgresTestcontainers.class)
class ControlPlaneApplicationTests {

    @Autowired
    JdbcClient jdbc;

    @Autowired
    MockMvcTester mvc;

    @Test
    void appliesSchemaOnStartup() {
        Integer changeSets = jdbc.sql("select count(*) from databasechangelog")
                .query(Integer.class)
                .single();
        assertThat(changeSets).isGreaterThanOrEqualTo(4);
    }

    @Test
    void servesSwaggerUiOnTopOfTheContractDocument() {
        assertThat(mvc.get().uri("/swagger-ui")).hasStatus3xxRedirection().hasRedirectedUrl("/swagger-ui/index.html");
        assertThat(mvc.get().uri("/swagger-ui/index.html"))
                .hasStatusOk()
                .bodyText()
                .contains("/api/v1/openapi.yaml");
        assertThat(mvc.get().uri("/webjars/swagger-ui/swagger-ui-bundle.js")).hasStatusOk();
        assertThat(mvc.get().uri("/v3/api-docs").accept(MediaType.APPLICATION_JSON))
                .hasStatus(404);
    }

    @Test
    void servesTheUiBundleWithASinglePageFallback() {
        assertThat(mvc.get().uri("/").accept(MediaType.TEXT_HTML)).hasForwardedUrl("index.html");
        assertThat(mvc.get().uri("/index.html"))
                .hasStatusOk()
                .hasHeader("Cache-Control", "no-cache")
                .bodyText()
                .contains("<div id=\"root\"></div>");
        assertThat(mvc.get().uri("/projects/webshop/PROD/k8s-prod-eu1").accept(MediaType.TEXT_HTML))
                .hasStatusOk()
                .bodyText()
                .contains("<div id=\"root\"></div>");
        assertThat(mvc.get().uri("/projects/webshop").accept(MediaType.APPLICATION_JSON))
                .hasStatus(404)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(mvc.get().uri("/assets/missing.js")).hasStatus(404);
        assertThat(mvc.get().uri("/api/v1/missing"))
                .hasStatus(404)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(mvc.get().uri("/actuator/missing")).hasStatus(404);
    }
}
