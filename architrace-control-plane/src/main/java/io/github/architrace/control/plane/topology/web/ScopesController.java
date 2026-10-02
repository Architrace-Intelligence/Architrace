/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology.web;

import io.github.architrace.control.plane.api.ScopesApi;
import io.github.architrace.control.plane.api.model.NodeViewDto;
import io.github.architrace.control.plane.api.model.ScopeSummaryDto;
import io.github.architrace.control.plane.api.model.TopologyGraphDto;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.TopologyQuery;
import io.github.architrace.control.plane.web.ApiPaths;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BASE)
class ScopesController implements ScopesApi {

    private final TopologyQuery query;
    private final Clock clock;

    ScopesController(TopologyQuery query, Clock clock) {
        this.query = query;
        this.clock = clock;
    }

    @Override
    public List<ScopeSummaryDto> listScopes() {
        return query.scopes().stream().map(ApiModels::toDto).toList();
    }

    @Override
    public TopologyGraphDto getGraph(String project, String environment, String cluster, OffsetDateTime at) {
        return ApiModels.toDto(query.currentGraph(new Scope(project, environment, cluster), atOrNow(at)));
    }

    @Override
    public List<NodeViewDto> listServices(String project, String environment, String cluster, OffsetDateTime at) {
        return query.services(new Scope(project, environment, cluster), atOrNow(at)).stream()
                .map(ApiModels::toDto)
                .toList();
    }

    private Instant atOrNow(OffsetDateTime at) {
        return ApiModels.toInstant(at).orElseGet(clock::instant);
    }
}
