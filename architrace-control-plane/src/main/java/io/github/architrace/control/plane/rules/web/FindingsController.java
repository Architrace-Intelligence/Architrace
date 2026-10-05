/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules.web;

import io.github.architrace.control.plane.api.FindingsApi;
import io.github.architrace.control.plane.api.model.FindingDto;
import io.github.architrace.control.plane.api.model.ImpactDto;
import io.github.architrace.control.plane.api.model.SeverityDto;
import io.github.architrace.control.plane.rules.FindingsQuery;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.web.ApiModels;
import io.github.architrace.control.plane.web.ApiPaths;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BASE)
class FindingsController implements FindingsApi {

    private final FindingsQuery query;
    private final Clock clock;

    FindingsController(FindingsQuery query, Clock clock) {
        this.query = query;
        this.clock = clock;
    }

    @Override
    public List<FindingDto> listFindings(
            String project, String environment, String cluster, SeverityDto severity, String rule) {
        Scope scope = new Scope(project, environment, cluster);
        return query
                .findings(
                        scope, Optional.ofNullable(severity).map(FindingsModels::toSeverity), Optional.ofNullable(rule))
                .stream()
                .map(FindingsModels::toDto)
                .toList();
    }

    @Override
    public ImpactDto getImpact(String project, String environment, String cluster, String node, OffsetDateTime at) {
        Scope scope = new Scope(project, environment, cluster);
        return FindingsModels.toDto(
                query.impact(scope, node, ApiModels.toInstant(at).orElseGet(clock::instant)));
    }
}
