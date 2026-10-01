/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

interface SnapshotRepository extends CrudRepository<SnapshotRow, Long> {

  @Query(
      """
      select distinct on (agent_id) *
      from snapshot
      where project = :project
        and environment = :environment
        and cluster = :cluster
        and window_end <= :at
      order by agent_id, window_end desc, id desc
      """)
  List<SnapshotRow> findLatestPerAgent(
      @Param("project") String project,
      @Param("environment") String environment,
      @Param("cluster") String cluster,
      @Param("at") Instant at);

  @Modifying
  @Query(
      """
      delete from snapshot
      where id in (
        select id from snapshot
        where window_end < :cutoff
        order by window_end, id
        limit :limit)
      """)
  int deleteOlderThan(@Param("cutoff") Instant cutoff, @Param("limit") int limit);
}
