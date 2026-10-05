/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules.persistence;

import io.github.architrace.control.plane.rules.Finding;
import io.github.architrace.control.plane.rules.FindingCounts;
import io.github.architrace.control.plane.rules.FindingStore;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.persistence.JsonDocument;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Component
class JdbcFindingStore implements FindingStore {

    private final FindingRepository repository;
    private final JsonMapper mapper = JsonMapper.builder().build();

    JdbcFindingStore(FindingRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void replace(Scope scope, List<Finding> findings) {
        repository.deleteByScope(scope.project(), scope.environment(), scope.cluster());
        repository.saveAll(findings.stream().map(this::toRow).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Finding> findings(Scope scope) {
        return repository.findByScope(scope.project(), scope.environment(), scope.cluster()).stream()
                .map(this::toFinding)
                .sorted(Finding.ORDER)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Scope, FindingCounts> counts() {
        return StreamSupport.stream(repository.findAll().spliterator(), false)
                .collect(Collectors.groupingBy(
                        JdbcFindingStore::scope,
                        Collectors.collectingAndThen(
                                Collectors.mapping(
                                        row -> Finding.Severity.valueOf(row.severity()), Collectors.toList()),
                                severities -> FindingCounts.of(severities.stream()))));
    }

    private static Scope scope(FindingRow row) {
        return new Scope(row.project(), row.environment(), row.cluster());
    }

    private FindingRow toRow(Finding finding) {
        return new FindingRow(
                null,
                finding.scope().project(),
                finding.scope().environment(),
                finding.scope().cluster(),
                finding.ruleId(),
                finding.severity().name(),
                encode(finding.subjectNodeIds()),
                finding.title(),
                finding.detail(),
                encode(finding.evidence()),
                finding.evaluatedAt());
    }

    private Finding toFinding(FindingRow row) {
        return new Finding(
                row.ruleId(),
                Finding.Severity.valueOf(row.severity()),
                scope(row),
                decode(row.subjectIds()),
                row.title(),
                row.detail(),
                decode(row.evidence()),
                row.evaluatedAt());
    }

    private JsonDocument encode(List<String> ids) {
        return new JsonDocument(mapper.writeValueAsString(ids));
    }

    private List<String> decode(JsonDocument document) {
        return List.of(mapper.readValue(document.json(), String[].class));
    }
}
