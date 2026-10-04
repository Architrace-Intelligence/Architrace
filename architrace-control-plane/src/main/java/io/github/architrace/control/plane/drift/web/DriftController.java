/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.drift.web;

import io.github.architrace.control.plane.api.DriftApi;
import io.github.architrace.control.plane.api.model.TopologyDiffDto;
import io.github.architrace.control.plane.drift.DriftQuery;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.web.ApiModels;
import io.github.architrace.control.plane.web.ApiPaths;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BASE)
class DriftController implements DriftApi {

    private final DriftQuery drift;
    private final Clock clock;

    DriftController(DriftQuery drift, Clock clock) {
        this.drift = drift;
        this.clock = clock;
    }

    @Override
    public TopologyDiffDto diffEnvironments(
            String project,
            String environment,
            String cluster,
            String leftEnvironment,
            String leftCluster,
            OffsetDateTime at) {
        Scope left = new Scope(project, leftEnvironment, leftCluster);
        Scope right = new Scope(project, environment, cluster);
        return DriftModels.toDto(drift.environments(left, right, atOrNow(at)));
    }

    @Override
    public TopologyDiffDto diffTimeline(
            String project, String environment, String cluster, OffsetDateTime from, OffsetDateTime to) {
        Scope scope = new Scope(project, environment, cluster);
        return DriftModels.toDto(drift.timeline(scope, from.toInstant(), atOrNow(to)));
    }

    private Instant atOrNow(OffsetDateTime at) {
        return ApiModels.toInstant(at).orElseGet(clock::instant);
    }
}
