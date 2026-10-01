/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.persistence;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

interface AgentRepository extends ListCrudRepository<AgentRow, Long> {

  Optional<AgentRow> findByNameAndProjectAndEnvironmentAndCluster(
      String name, String project, String environment, String cluster);

  @Modifying
  @Query("update agent set last_seen_at = :now where id = :id")
  void touch(@Param("id") long id, @Param("now") Instant now);
}
