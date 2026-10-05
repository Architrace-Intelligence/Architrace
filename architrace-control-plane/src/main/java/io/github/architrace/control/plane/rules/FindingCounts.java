/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record FindingCounts(int high, int medium, int low) {

    public static final FindingCounts NONE = new FindingCounts(0, 0, 0);

    public FindingCounts {
        if (high < 0 || medium < 0 || low < 0) {
            throw new IllegalArgumentException("counts must not be negative");
        }
    }

    public static FindingCounts of(Stream<Finding.Severity> severities) {
        Map<Finding.Severity, Long> bySeverity =
                severities.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        return new FindingCounts(
                count(bySeverity, Finding.Severity.HIGH),
                count(bySeverity, Finding.Severity.MEDIUM),
                count(bySeverity, Finding.Severity.LOW));
    }

    public int total() {
        return high + medium + low;
    }

    private static int count(Map<Finding.Severity, Long> bySeverity, Finding.Severity severity) {
        return bySeverity.getOrDefault(severity, 0L).intValue();
    }
}
