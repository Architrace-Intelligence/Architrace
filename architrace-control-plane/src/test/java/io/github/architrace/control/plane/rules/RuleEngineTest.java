/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import static io.github.architrace.control.plane.topology.TestTopology.NOW;
import static io.github.architrace.control.plane.topology.TestTopology.SCOPE;
import static io.github.architrace.control.plane.topology.TestTopology.database;
import static io.github.architrace.control.plane.topology.TestTopology.edge;
import static io.github.architrace.control.plane.topology.TestTopology.service;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import io.github.architrace.control.plane.rules.RulesProperties.SharedDatabaseProperties;
import io.github.architrace.control.plane.rules.RulesProperties.UnknownExternalProperties;
import io.github.architrace.control.plane.topology.EdgeKind;
import io.github.architrace.control.plane.topology.NodeAttributes;
import io.github.architrace.control.plane.topology.NodeType;
import io.github.architrace.control.plane.topology.TopologyGraph;
import io.github.architrace.control.plane.topology.TopologyNode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class RuleEngineTest {

    private final TopologyNode orders = service("orders", "1.0.0", "orders");
    private final TopologyNode payments = service("payments", "1.0.0", "payments");
    private final TopologyNode ordersDb = database("orders");
    private final TopologyNode stripe =
            new TopologyNode("ext:api.stripe.com", NodeType.EXTERNAL, "api.stripe.com", NodeAttributes.none());

    @Test
    void buildsTheRulesFromTheProperties() {
        RulesProperties properties = new RulesProperties(
                new SharedDatabaseProperties(3), new UnknownExternalProperties(List.of("api.github.com")));

        RuleEngine engine = RuleEngine.of(properties);

        assertThat(engine.rules())
                .containsExactly(
                        new CyclicDependency(), new SharedDatabase(3), new UnknownExternal(Set.of("api.github.com")));
    }

    @Test
    void ordersFindingsBySeverityThenRuleThenSubject() {
        TopologyGraph graph = new TopologyGraph(
                SCOPE,
                NOW,
                List.of(orders, payments, ordersDb, stripe),
                List.of(
                        edge(orders, payments, EdgeKind.SYNC, 10),
                        edge(payments, orders, EdgeKind.SYNC, 10),
                        edge(orders, ordersDb, EdgeKind.SYNC, 10),
                        edge(payments, ordersDb, EdgeKind.SYNC, 10),
                        edge(payments, stripe, EdgeKind.SYNC, 10)));

        List<Finding> findings = RuleEngine.of(RulesProperties.defaults()).evaluate(graph);

        assertThat(findings)
                .extracting(Finding::ruleId, Finding::severity, Finding::subjectNodeIds)
                .containsExactly(
                        tuple(
                                "cyclic-dependency",
                                Finding.Severity.HIGH,
                                List.of("service:orders", "service:payments")),
                        tuple("shared-database", Finding.Severity.HIGH, List.of("db:postgresql/orders")),
                        tuple("unknown-external", Finding.Severity.LOW, List.of("ext:api.stripe.com")));
    }

    @Test
    void anEmptyGraphHasNoFindings() {
        TopologyGraph graph = new TopologyGraph(SCOPE, NOW, List.of(), List.of());

        assertThat(RuleEngine.of(RulesProperties.defaults()).evaluate(graph)).isEmpty();
        assertThat(new RuleEngine(List.of()).evaluate(graph)).isEmpty();
    }

    @Test
    void propertiesBindWithDefaultsAndOverrides() {
        RulesProperties defaults = bind(Map.of());
        RulesProperties overridden = bind(Map.of(
                "architrace.rules.shared-database.min-services", "4",
                "architrace.rules.unknown-external.allowlist", "api.github.com,api.stripe.com"));

        assertThat(defaults).isEqualTo(RulesProperties.defaults());
        assertThat(defaults.sharedDatabase().minServices()).isEqualTo(2);
        assertThat(defaults.unknownExternal().allowlist()).isEmpty();
        assertThat(overridden.sharedDatabase().minServices()).isEqualTo(4);
        assertThat(overridden.unknownExternal().allowlist()).containsExactly("api.github.com", "api.stripe.com");
    }

    @Test
    void propertiesRejectAThresholdBelowTwo() {
        assertThatThrownBy(() -> new SharedDatabaseProperties(1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("minServices must be at least 2");
    }

    @Test
    void findingRequiresASubjectAndText() {
        List<String> noNodes = List.of();
        List<String> subject = List.of("db:postgresql/orders");

        assertThatThrownBy(() -> new Finding(
                        "shared-database", Finding.Severity.HIGH, SCOPE, noNodes, "title", "detail", noNodes, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("subjectNodeIds must not be empty");
        assertThatThrownBy(() -> new Finding(
                        "shared-database", Finding.Severity.HIGH, SCOPE, subject, " ", "detail", noNodes, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title must not be blank");
    }

    private static RulesProperties bind(Map<String, String> properties) {
        return new Binder(new MapConfigurationPropertySource(properties))
                .bindOrCreate("architrace.rules", RulesProperties.class);
    }
}
