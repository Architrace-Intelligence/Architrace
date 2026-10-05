/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.control.plane.PostgresTestcontainers;
import io.github.architrace.control.plane.rules.Finding;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.persistence.PersistenceConfiguration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJdbcTest(includeFilters = {})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestcontainers.class, PersistenceConfiguration.class, JdbcFindingStore.class})
class JdbcFindingStoreTest {

    private static final Scope PROD = new Scope("webshop", "PROD", "k8s-prod-eu1");
    private static final Scope DEV = new Scope("webshop", "DEV", "k8s-dev");
    private static final Instant NOW = Instant.parse("2026-10-05T12:00:00Z");

    @Autowired
    JdbcFindingStore store;

    @Test
    void replacesTheFindingsOfOneScopeAndLeavesTheOthersAlone() {
        Finding cycle = finding(PROD, "cyclic-dependency", Finding.Severity.HIGH, "service:sales/orders", NOW);
        Finding external = finding(PROD, "unknown-external", Finding.Severity.LOW, "ext:api.stripe.com", NOW);
        Finding devCycle = finding(DEV, "cyclic-dependency", Finding.Severity.HIGH, "service:sales/orders", NOW);
        store.replace(PROD, List.of(external, cycle));
        store.replace(DEV, List.of(devCycle));

        Finding later =
                finding(PROD, "shared-database", Finding.Severity.HIGH, "db:postgresql/orders", NOW.plusSeconds(60));
        store.replace(PROD, List.of(later));

        assertThat(store.findings(PROD)).containsExactly(later);
        assertThat(store.findings(DEV)).containsExactly(devCycle);
    }

    @Test
    void readsFindingsBackInSeverityRuleAndSubjectOrderWithTheirIdLists() {
        Finding medium = finding(PROD, "fan-in-hub", Finding.Severity.MEDIUM, "service:sales/payments", NOW);
        Finding low = finding(PROD, "unknown-external", Finding.Severity.LOW, "ext:api.stripe.com", NOW);
        Finding highLater = finding(PROD, "shared-database", Finding.Severity.HIGH, "db:postgresql/orders", NOW);
        Finding highFirst = finding(PROD, "cyclic-dependency", Finding.Severity.HIGH, "service:sales/orders", NOW);
        store.replace(PROD, List.of(low, medium, highLater, highFirst));

        assertThat(store.findings(PROD)).containsExactly(highFirst, highLater, medium, low);
        assertThat(store.findings(DEV)).isEmpty();
    }

    @Test
    void replacingWithNothingClearsTheScope() {
        store.replace(
                PROD, List.of(finding(PROD, "fan-in-hub", Finding.Severity.MEDIUM, "service:sales/payments", NOW)));

        store.replace(PROD, List.of());

        assertThat(store.findings(PROD)).isEmpty();
    }

    private static Finding finding(Scope scope, String ruleId, Finding.Severity severity, String subject, Instant at) {
        return new Finding(
                ruleId,
                severity,
                scope,
                List.of(subject),
                ruleId + " on " + subject,
                "detail of " + ruleId,
                List.of("service:sales/checkout", "service:sales/orders"),
                at);
    }
}
