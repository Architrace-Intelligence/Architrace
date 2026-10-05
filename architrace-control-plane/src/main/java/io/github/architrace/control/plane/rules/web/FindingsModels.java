/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules.web;

import io.github.architrace.control.plane.api.model.FindingDto;
import io.github.architrace.control.plane.api.model.ImpactDto;
import io.github.architrace.control.plane.api.model.ImpactedNodeDto;
import io.github.architrace.control.plane.api.model.SeverityDto;
import io.github.architrace.control.plane.rules.Finding;
import io.github.architrace.control.plane.rules.Impact;
import io.github.architrace.control.plane.topology.web.ApiModels;

final class FindingsModels {

    private FindingsModels() {}

    static FindingDto toDto(Finding finding) {
        return new FindingDto(
                finding.ruleId(),
                toDto(finding.severity()),
                ApiModels.toDto(finding.scope()),
                finding.subjectNodeIds(),
                finding.title(),
                finding.detail(),
                finding.evidence(),
                ApiModels.atUtc(finding.evaluatedAt()));
    }

    static SeverityDto toDto(Finding.Severity severity) {
        return SeverityDto.valueOf(severity.name());
    }

    static ImpactDto toDto(Impact impact) {
        return new ImpactDto(
                ApiModels.toDto(impact.subject()),
                ApiModels.atUtc(impact.at()),
                impact.impaired().stream().map(FindingsModels::toDto).toList(),
                impact.delayed().stream().map(FindingsModels::toDto).toList(),
                impact.services(),
                impact.servicesTotal());
    }

    static ImpactedNodeDto toDto(Impact.ImpactedNode impacted) {
        return new ImpactedNodeDto(ApiModels.toDto(impacted.node()), impacted.distance(), impacted.path());
    }

    static Finding.Severity toSeverity(SeverityDto severity) {
        return Finding.Severity.valueOf(severity.name());
    }
}
