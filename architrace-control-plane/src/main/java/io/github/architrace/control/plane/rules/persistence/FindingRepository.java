/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules.persistence;

import java.util.List;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

interface FindingRepository extends CrudRepository<FindingRow, Long> {

    @Query("""
      select *
      from finding
      where project = :project
        and environment = :environment
        and cluster = :cluster
      """)
    List<FindingRow> findByScope(
            @Param("project") String project,
            @Param("environment") String environment,
            @Param("cluster") String cluster);

    @Modifying
    @Query("""
      delete from finding
      where project = :project
        and environment = :environment
        and cluster = :cluster
      """)
    int deleteByScope(
            @Param("project") String project,
            @Param("environment") String environment,
            @Param("cluster") String cluster);
}
