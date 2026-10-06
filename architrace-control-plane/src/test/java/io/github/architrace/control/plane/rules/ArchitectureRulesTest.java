/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import static io.github.architrace.control.plane.topology.TestTopology.NOW;
import static io.github.architrace.control.plane.topology.TestTopology.SCOPE;
import static io.github.architrace.control.plane.topology.TestTopology.database;
import static io.github.architrace.control.plane.topology.TestTopology.edge;
import static io.github.architrace.control.plane.topology.TestTopology.topic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.architrace.control.plane.topology.EdgeKind;
import io.github.architrace.control.plane.topology.NodeAttributes;
import io.github.architrace.control.plane.topology.NodeType;
import io.github.architrace.control.plane.topology.PlatformHosts;
import io.github.architrace.control.plane.topology.TestTopology;
import io.github.architrace.control.plane.topology.TopologyEdge;
import io.github.architrace.control.plane.topology.TopologyGraph;
import io.github.architrace.control.plane.topology.TopologyNode;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ArchitectureRulesTest {

    private final TopologyNode checkout = service("checkout");
    private final TopologyNode orders = service("orders");
    private final TopologyNode payments = service("payments");
    private final TopologyNode inventory = service("inventory");
    private final TopologyNode billing = service("billing");
    private final TopologyNode reporting = service("reporting");
    private final TopologyNode ordersDb = database("orders");
    private final TopologyNode events = topic("order-events");
    private final TopologyNode stripe = external("api.stripe.com");
    private final TopologyNode github = external("api.github.com");

    private static TopologyNode service(String name) {
        return TestTopology.service(name, "1.0.0", name);
    }

    private static TopologyNode service(String domain, String name) {
        return new TopologyNode(
                "service:" + domain + "/" + name,
                NodeType.SERVICE,
                name,
                service(name).attributes());
    }

    private static TopologyNode external(String address) {
        return new TopologyNode("ext:" + address, NodeType.EXTERNAL, address, NodeAttributes.none());
    }

    private static TopologyEdge sync(TopologyNode source, TopologyNode target) {
        return edge(source, target, EdgeKind.SYNC, 10);
    }

    private static TopologyGraph graph(List<TopologyNode> nodes, List<TopologyEdge> edges) {
        return new TopologyGraph(SCOPE, NOW, nodes, edges);
    }

    @Nested
    class CyclicDependencyRule {

        private final CyclicDependency rule = new CyclicDependency();

        @Test
        void reportsEveryStronglyConnectedComponentWithItsShortestCycleAsEvidence() {
            TopologyNode auth = service("auth");
            TopologyNode users = service("users");
            TopologyGraph graph = graph(
                    List.of(checkout, orders, payments, inventory, auth, users),
                    List.of(
                            sync(checkout, orders),
                            sync(orders, payments),
                            sync(payments, orders),
                            sync(orders, inventory),
                            sync(inventory, payments),
                            sync(auth, users),
                            sync(users, auth)));

            List<Finding> findings = rule.evaluate(graph);

            assertThat(findings).hasSize(2);
            Finding authCycle = findings.getFirst();
            assertThat(authCycle.ruleId()).isEqualTo("cyclic-dependency");
            assertThat(authCycle.severity()).isEqualTo(Finding.Severity.HIGH);
            assertThat(authCycle.scope()).isEqualTo(SCOPE);
            assertThat(authCycle.evaluatedAt()).isEqualTo(NOW);
            assertThat(authCycle.subjectNodeIds()).containsExactly("service:auth", "service:users");
            assertThat(authCycle.evidence()).containsExactly("service:auth", "service:users");
            assertThat(authCycle.title()).isEqualTo("Cyclic dependency between 2 services");
            assertThat(authCycle.detail()).startsWith("auth -> users -> auth over synchronous calls");
            Finding orderCycle = findings.getLast();
            assertThat(orderCycle.subjectNodeIds())
                    .containsExactly("service:inventory", "service:orders", "service:payments");
            assertThat(orderCycle.evidence())
                    .containsExactly("service:inventory", "service:payments", "service:orders");
            assertThat(orderCycle.detail()).startsWith("inventory -> payments -> orders -> inventory");
        }

        @Test
        void ignoresSelfCallsAndCyclesThatCloseThroughATopic() {
            TopologyGraph graph = graph(
                    List.of(checkout, orders, ordersDb, events, reporting),
                    List.of(
                            sync(checkout, orders),
                            sync(orders, orders),
                            sync(orders, ordersDb),
                            edge(orders, events, EdgeKind.PUBLISH, 5),
                            edge(events, reporting, EdgeKind.CONSUME, 5),
                            sync(reporting, orders)));

            assertThat(rule.evaluate(graph)).isEmpty();
        }
    }

    @Nested
    class SharedDatabaseRule {

        @Test
        void reportsADatabaseUsedByAtLeastTheConfiguredNumberOfServices() {
            TopologyNode checkoutDb = database("checkout");
            TopologyGraph graph = graph(
                    List.of(orders, billing, reporting, checkout, ordersDb, checkoutDb),
                    List.of(
                            sync(reporting, ordersDb),
                            sync(orders, ordersDb),
                            sync(billing, ordersDb),
                            sync(checkout, checkoutDb),
                            sync(checkout, orders)));

            List<Finding> findings = new SharedDatabase(2).evaluate(graph);

            assertThat(findings).hasSize(1);
            Finding finding = findings.getFirst();
            assertThat(finding.ruleId()).isEqualTo("shared-database");
            assertThat(finding.severity()).isEqualTo(Finding.Severity.HIGH);
            assertThat(finding.subjectNodeIds()).containsExactly("db:postgresql/orders");
            assertThat(finding.evidence()).containsExactly("service:billing", "service:orders", "service:reporting");
            assertThat(finding.title()).isEqualTo("Database orders is shared by 3 services");
            assertThat(finding.detail()).startsWith("billing, orders, reporting use the database orders directly");
        }

        @Test
        void countsOnlyServicesBehindSynchronousEdgesAgainstTheThreshold() {
            TopologyGraph graph = graph(
                    List.of(orders, billing, reporting, ordersDb, events, stripe),
                    List.of(
                            sync(orders, ordersDb),
                            sync(billing, ordersDb),
                            edge(reporting, ordersDb, EdgeKind.PUBLISH, 1),
                            edge(orders, events, EdgeKind.PUBLISH, 1),
                            edge(billing, events, EdgeKind.PUBLISH, 1),
                            sync(orders, stripe),
                            sync(billing, stripe)));

            assertThat(new SharedDatabase(3).evaluate(graph)).isEmpty();
            assertThat(new SharedDatabase(2).evaluate(graph))
                    .extracting(Finding::subjectNodeIds)
                    .containsExactly(List.of("db:postgresql/orders"));
        }

        @Test
        void rejectsAThresholdBelowTwo() {
            assertThatThrownBy(() -> new SharedDatabase(1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("minServices must be at least 2");
        }
    }

    @Nested
    class WideBlastRadiusRule {

        private final TopologyNode authDb = database("auth");
        private final TopologyNode standalone = service("standalone");
        private final TopologyGraph graph = graph(
                List.of(checkout, orders, payments, inventory, standalone, authDb),
                List.of(
                        sync(checkout, orders),
                        sync(checkout, payments),
                        sync(checkout, inventory),
                        sync(orders, authDb),
                        sync(payments, authDb),
                        sync(inventory, authDb)));

        @Test
        void reportsANodeWhoseFailureImpairsMoreThanTheShareOfServices() {
            List<Finding> findings = new WideBlastRadius(50, 3).evaluate(graph);

            assertThat(findings).hasSize(1);
            Finding finding = findings.getFirst();
            assertThat(finding.ruleId()).isEqualTo("wide-blast-radius");
            assertThat(finding.severity()).isEqualTo(Finding.Severity.HIGH);
            assertThat(finding.subjectNodeIds()).containsExactly("db:postgresql/auth");
            assertThat(finding.evidence())
                    .containsExactly("service:inventory", "service:orders", "service:payments", "service:checkout");
            assertThat(finding.title()).isEqualTo("Failure of auth impairs 4 of 5 services");
            assertThat(finding.detail())
                    .isEqualTo("If auth fails, 80 % of the services of the scope stop working: inventory, orders,"
                            + " payments, checkout");
        }

        @Test
        void staysQuietBelowTheShareOrTheMinimumNumberOfServices() {
            assertThat(new WideBlastRadius(80, 3).evaluate(graph)).isEmpty();
            assertThat(new WideBlastRadius(50, 5).evaluate(graph)).isEmpty();
            assertThat(new WideBlastRadius(79, 4).evaluate(graph)).hasSize(1);
        }

        @Test
        void rejectsThresholdsOutsideTheirRange() {
            assertThatThrownBy(() -> new WideBlastRadius(0, 3)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new WideBlastRadius(101, 3)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new WideBlastRadius(50, 0)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class CrossDomainCouplingRule {

        private final TopologyNode salesOrders = service("sales", "orders");
        private final TopologyNode salesCatalog = service("sales", "catalog");
        private final TopologyNode salesCheckout = service("sales", "checkout");
        private final TopologyNode financeBilling = service("finance", "billing");
        private final TopologyNode customersCrm = service("customers", "crm");
        private final TopologyNode warehouseStock = service("warehouse", "stock");
        private final TopologyNode logisticsShipping = service("logistics", "shipping");
        private final TopologyGraph graph = graph(
                List.of(
                        salesOrders,
                        salesCatalog,
                        salesCheckout,
                        financeBilling,
                        customersCrm,
                        warehouseStock,
                        logisticsShipping,
                        ordersDb),
                List.of(
                        sync(salesOrders, salesCatalog),
                        sync(salesOrders, financeBilling),
                        sync(salesOrders, customersCrm),
                        sync(salesOrders, warehouseStock),
                        sync(salesOrders, logisticsShipping),
                        sync(salesOrders, ordersDb),
                        sync(salesCheckout, salesOrders),
                        sync(salesCheckout, financeBilling)));

        @Test
        void reportsAServiceCallingIntoMoreDomainsThanAllowed() {
            List<Finding> findings = new CrossDomainCoupling(3).evaluate(graph);

            assertThat(findings).hasSize(1);
            Finding finding = findings.getFirst();
            assertThat(finding.ruleId()).isEqualTo("cross-domain-coupling");
            assertThat(finding.severity()).isEqualTo(Finding.Severity.MEDIUM);
            assertThat(finding.subjectNodeIds()).containsExactly("service:sales/orders");
            assertThat(finding.evidence())
                    .containsExactly(
                            "service:customers/crm",
                            "service:finance/billing",
                            "service:logistics/shipping",
                            "service:warehouse/stock");
            assertThat(finding.title()).isEqualTo("orders calls into 4 domains");
            assertThat(finding.detail())
                    .startsWith("orders (sales) calls crm (customers), billing (finance), shipping (logistics),"
                            + " stock (warehouse) in 4 other domains");
        }

        @Test
        void countsDomainsNotCallsAndIgnoresServicesWithoutADomain() {
            assertThat(new CrossDomainCoupling(4).evaluate(graph)).isEmpty();
            TopologyGraph flat = graph(
                    List.of(orders, billing, payments, inventory),
                    List.of(sync(orders, billing), sync(orders, payments), sync(orders, inventory)));
            assertThat(new CrossDomainCoupling(1).evaluate(flat)).isEmpty();
            assertThatThrownBy(() -> new CrossDomainCoupling(0)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class FanInHubRule {

        private final TopologyGraph graph = graph(
                List.of(checkout, orders, billing, reporting, payments, ordersDb),
                List.of(
                        sync(checkout, payments),
                        sync(orders, payments),
                        sync(billing, payments),
                        sync(payments, payments),
                        sync(payments, ordersDb),
                        sync(reporting, ordersDb),
                        sync(checkout, ordersDb),
                        sync(orders, ordersDb)));

        @Test
        void reportsAServiceWithMoreDirectCallersThanAllowed() {
            List<Finding> findings = new FanInHub(2).evaluate(graph);

            assertThat(findings).hasSize(1);
            Finding finding = findings.getFirst();
            assertThat(finding.ruleId()).isEqualTo("fan-in-hub");
            assertThat(finding.severity()).isEqualTo(Finding.Severity.MEDIUM);
            assertThat(finding.subjectNodeIds()).containsExactly("service:payments");
            assertThat(finding.evidence()).containsExactly("service:billing", "service:checkout", "service:orders");
            assertThat(finding.title()).isEqualTo("payments has 3 direct callers");
            assertThat(finding.detail()).startsWith("3 services call payments synchronously");
        }

        @Test
        void staysQuietAtTheThresholdAndNeverCountsDatabases() {
            assertThat(new FanInHub(3).evaluate(graph)).isEmpty();
            assertThatThrownBy(() -> new FanInHub(0)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class LongSyncChainRule {

        private final TopologyGraph graph = graph(
                List.of(checkout, inventory, orders, payments, billing, reporting, ordersDb),
                List.of(
                        sync(checkout, inventory),
                        sync(inventory, orders),
                        sync(orders, payments),
                        sync(payments, billing),
                        sync(billing, ordersDb),
                        sync(reporting, payments)));

        @Test
        void reportsTheLongestChainFromEveryEntryNodeAboveTheThreshold() {
            List<Finding> findings = new LongSyncChain(4).evaluate(graph);

            assertThat(findings).hasSize(1);
            Finding finding = findings.getFirst();
            assertThat(finding.ruleId()).isEqualTo("long-sync-chain");
            assertThat(finding.severity()).isEqualTo(Finding.Severity.MEDIUM);
            assertThat(finding.subjectNodeIds())
                    .containsExactly(
                            "service:checkout",
                            "service:inventory",
                            "service:orders",
                            "service:payments",
                            "service:billing",
                            "db:postgresql/orders");
            assertThat(finding.evidence()).isEqualTo(finding.subjectNodeIds());
            assertThat(finding.title()).isEqualTo("Synchronous chain of 5 hops from checkout");
            assertThat(finding.detail())
                    .startsWith("checkout -> inventory -> orders -> payments -> billing -> orders: every hop");
        }

        @Test
        void staysQuietAtTheThresholdAndReportsBothEntriesBelowIt() {
            assertThat(new LongSyncChain(5).evaluate(graph)).isEmpty();
            assertThat(new LongSyncChain(2).evaluate(graph))
                    .extracting(finding -> finding.subjectNodeIds().getFirst())
                    .containsExactly("service:checkout", "service:reporting");
            assertThatThrownBy(() -> new LongSyncChain(0)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void collapsesACycleToItsSmallestMemberAndPrefersTheSmallerBranchOnTies() {
            TopologyGraph graph = graph(
                    List.of(checkout, orders, payments, billing, inventory, ordersDb, stripe),
                    List.of(
                            sync(checkout, orders),
                            sync(orders, payments),
                            sync(payments, orders),
                            sync(payments, ordersDb),
                            sync(checkout, inventory),
                            sync(inventory, billing),
                            sync(billing, stripe)));

            List<Finding> findings = new LongSyncChain(1).evaluate(graph);

            assertThat(findings).hasSize(1);
            assertThat(findings.getFirst().subjectNodeIds())
                    .containsExactly("service:checkout", "service:inventory", "service:billing", "ext:api.stripe.com");
        }
    }

    @Nested
    class UnknownExternalRule {

        @Test
        void reportsExternalsOutsideTheAllowlistWithTheirCallers() {
            TopologyGraph graph = graph(
                    List.of(checkout, payments, orders, stripe, github),
                    List.of(sync(payments, stripe), sync(checkout, stripe), sync(orders, github)));

            List<Finding> findings = new UnknownExternal(Set.of("api.github.com")).evaluate(graph);

            assertThat(findings).hasSize(1);
            Finding finding = findings.getFirst();
            assertThat(finding.ruleId()).isEqualTo("unknown-external");
            assertThat(finding.severity()).isEqualTo(Finding.Severity.LOW);
            assertThat(finding.subjectNodeIds()).containsExactly("ext:api.stripe.com");
            assertThat(finding.evidence()).containsExactly("service:checkout", "service:payments");
            assertThat(finding.title()).isEqualTo("Unknown external system api.stripe.com");
            assertThat(finding.detail())
                    .isEqualTo("checkout, payments call api.stripe.com, which is not on the allowlist of known external"
                            + " systems");
        }

        @Test
        void reportsEveryExternalInIdOrderWhenTheAllowlistIsEmpty() {
            TopologyGraph graph = graph(
                    List.of(orders, stripe, github, ordersDb), List.of(sync(orders, stripe), sync(orders, ordersDb)));

            List<Finding> findings = new UnknownExternal(Set.of()).evaluate(graph);

            assertThat(findings)
                    .extracting(Finding::subjectNodeIds)
                    .containsExactly(List.of("ext:api.github.com"), List.of("ext:api.stripe.com"));
            assertThat(findings.getFirst().evidence()).isEmpty();
            assertThat(findings.getFirst().detail())
                    .isEqualTo("api.github.com is not on the allowlist of known external systems");
        }

        @Test
        void treatsTheConfiguredPlatformHostsAsKnown() {
            TopologyNode flags = external("flags.platform.internal");
            TopologyGraph graph = new PlatformHosts(Set.of("flags.platform.internal"))
                    .classify(graph(
                            List.of(checkout, orders, flags, stripe),
                            List.of(sync(checkout, flags), sync(orders, flags), sync(orders, stripe))));

            List<Finding> findings = new UnknownExternal(Set.of()).evaluate(graph);

            assertThat(findings).extracting(Finding::subjectNodeIds).containsExactly(List.of("ext:api.stripe.com"));
        }
    }

    @Nested
    class DataStreamWithoutProducerRule {

        private final DataStreamWithoutProducer rule = new DataStreamWithoutProducer();

        @Test
        void reportsATopicThatIsConsumedButNeverPublishedWithItsConsumersAsEvidence() {
            TopologyGraph graph = graph(
                    List.of(orders, ordersDb, events, reporting, billing),
                    List.of(
                            sync(orders, ordersDb),
                            edge(events, reporting, EdgeKind.CONSUME, 5),
                            edge(events, billing, EdgeKind.CONSUME, 5)));

            List<Finding> findings = rule.evaluate(graph);

            assertThat(findings).hasSize(1);
            Finding finding = findings.getFirst();
            assertThat(finding.ruleId()).isEqualTo("data-stream-without-producer");
            assertThat(finding.severity()).isEqualTo(Finding.Severity.LOW);
            assertThat(finding.subjectNodeIds()).containsExactly(events.id());
            assertThat(finding.evidence()).containsExactly(billing.id(), reporting.id());
            assertThat(finding.title()).isEqualTo("Data stream order-events has consumers but no producer");
            assertThat(finding.detail())
                    .isEqualTo("billing, reporting consume order-events; no service publishes to it in the observed"
                            + " traces. A topic filled by change data capture from an outbox table looks like this:"
                            + " the producing service only writes to its database");
        }

        @Test
        void staysQuietForAPublishedTopicAndForATopicNobodyConsumes() {
            TopologyGraph published = graph(
                    List.of(orders, events, reporting),
                    List.of(edge(orders, events, EdgeKind.PUBLISH, 5), edge(events, reporting, EdgeKind.CONSUME, 5)));
            TopologyGraph idle = graph(List.of(orders, events), List.of());

            assertThat(rule.evaluate(published)).isEmpty();
            assertThat(rule.evaluate(idle)).isEmpty();
        }
    }
}
