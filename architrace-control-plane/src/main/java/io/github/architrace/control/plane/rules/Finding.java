/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import io.github.architrace.control.plane.topology.Scope;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record Finding(
        String ruleId,
        Severity severity,
        Scope scope,
        List<String> subjectNodeIds,
        String title,
        String detail,
        List<String> evidence,
        Instant evaluatedAt) {

    public static final Comparator<Finding> ORDER = Comparator.comparing(Finding::severity)
            .thenComparing(Finding::ruleId)
            .thenComparing(finding -> String.join(",", finding.subjectNodeIds()));

    public Finding {
        requireText(ruleId, "ruleId");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(scope, "scope");
        subjectNodeIds = List.copyOf(subjectNodeIds);
        if (subjectNodeIds.isEmpty()) {
            throw new IllegalArgumentException("subjectNodeIds must not be empty");
        }
        requireText(title, "title");
        requireText(detail, "detail");
        evidence = List.copyOf(evidence);
        Objects.requireNonNull(evaluatedAt, "evaluatedAt");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    public enum Severity {
        HIGH,
        MEDIUM,
        LOW
    }
}
