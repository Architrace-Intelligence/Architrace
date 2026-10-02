/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology.web;

import io.github.architrace.control.plane.api.SnapshotsApi;
import io.github.architrace.control.plane.api.model.SnapshotDto;
import io.github.architrace.control.plane.api.model.SnapshotPageDto;
import io.github.architrace.control.plane.topology.PageRequest;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.SnapshotFilter;
import io.github.architrace.control.plane.topology.SnapshotId;
import io.github.architrace.control.plane.topology.TopologyQuery;
import io.github.architrace.control.plane.web.ApiPaths;
import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BASE)
class SnapshotsController implements SnapshotsApi {

    private final TopologyQuery query;

    SnapshotsController(TopologyQuery query) {
        this.query = query;
    }

    @Override
    public SnapshotPageDto listSnapshots(
            String project,
            String environment,
            String cluster,
            OffsetDateTime from,
            OffsetDateTime to,
            Integer page,
            Integer size) {
        SnapshotFilter filter = new SnapshotFilter(
                new Scope(project, environment, cluster), ApiModels.toInstant(from), ApiModels.toInstant(to));
        return ApiModels.toDto(query.snapshots(filter, new PageRequest(page, size)));
    }

    @Override
    public SnapshotDto getSnapshot(Long snapshotId) {
        SnapshotId id = new SnapshotId(snapshotId);
        return ApiModels.toDto(id, query.snapshot(id));
    }
}
