/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(properties = "spring.grpc.server.enabled=false")
@Import(PostgresTestcontainers.class)
class ControlPlaneApplicationTests {

  @Autowired JdbcClient jdbc;

  @Test
  void appliesSchemaOnStartup() {
    Integer changeSets =
        jdbc.sql("select count(*) from databasechangelog").query(Integer.class).single();
    assertThat(changeSets).isGreaterThanOrEqualTo(4);
  }
}
