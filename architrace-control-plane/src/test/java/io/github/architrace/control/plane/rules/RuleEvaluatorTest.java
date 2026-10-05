/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import static io.github.architrace.control.plane.topology.TestTopology.NOW;
import static io.github.architrace.control.plane.topology.TestTopology.SCOPE;
import static io.github.architrace.control.plane.topology.TestTopology.edge;
import static io.github.architrace.control.plane.topology.TestTopology.service;
import static io.github.architrace.control.plane.topology.TestTopology.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.github.architrace.control.plane.ingestion.SnapshotIngested;
import io.github.architrace.control.plane.topology.Agent;
import io.github.architrace.control.plane.topology.AgentLiveness;
import io.github.architrace.control.plane.topology.AgentRegistration;
import io.github.architrace.control.plane.topology.EdgeKind;
import io.github.architrace.control.plane.topology.InMemoryAgentStore;
import io.github.architrace.control.plane.topology.InMemorySnapshotStore;
import io.github.architrace.control.plane.topology.PlatformHosts;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.TopologyEdge;
import io.github.architrace.control.plane.topology.TopologyMetrics;
import io.github.architrace.control.plane.topology.TopologyNode;
import io.github.architrace.control.plane.topology.TopologyQuery;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class RuleEvaluatorTest {

    private static final Scope DEV = new Scope("webshop", "DEV", "k8s-dev");

    private final InMemoryAgentStore agents = new InMemoryAgentStore();
    private final InMemorySnapshotStore snapshots = new InMemorySnapshotStore();
    private final InMemoryFindingStore findings = new InMemoryFindingStore();
    private final SteppingClock clock = new SteppingClock(NOW);
    private final TopologyQuery topology = new TopologyQuery(
            agents,
            snapshots,
            PlatformHosts.none(),
            new AgentLiveness(Duration.ofSeconds(30)),
            new TopologyMetrics(new SimpleMeterRegistry()),
            clock);
    private final RuleEvaluator evaluator = new RuleEvaluator(
            topology, RuleEngine.of(RulesProperties.defaults()), findings, RulesProperties.defaults(), clock);

    private final TopologyNode orders = service("orders", "1.0.0", "orders");
    private final TopologyNode payments = service("payments", "1.0.0", "payments");

    @Test
    void evaluatesTheCurrentGraphOfTheScopeOnItsFirstSnapshot() {
        Agent agent = agents.register(new AgentRegistration("prod-eu1-a", "0.4.0", SCOPE), NOW.minusSeconds(120));
        snapshots.save(snapshot(agent.id(), NOW.minusSeconds(1), List.of(orders, payments), cycle()));

        evaluator.onSnapshotIngested(new SnapshotIngested(SCOPE, NOW));

        assertThat(findings.findings(SCOPE)).singleElement().satisfies(finding -> {
            assertThat(finding.ruleId()).isEqualTo("cyclic-dependency");
            assertThat(finding.scope()).isEqualTo(SCOPE);
            assertThat(finding.evaluatedAt()).isEqualTo(NOW);
        });
    }

    @Test
    void evaluatesAScopeAtMostOncePerIntervalAndCatchesUpAfterwards() {
        Agent agent = agents.register(new AgentRegistration("prod-eu1-a", "0.4.0", SCOPE), NOW.minusSeconds(120));
        snapshots.save(snapshot(agent.id(), NOW.minusSeconds(1), List.of(orders, payments), cycle()));

        assertThat(evaluator.evaluate(SCOPE)).isPresent();
        snapshots.save(snapshot(agent.id(), NOW.plusSeconds(10), List.of(orders, payments), List.of()));
        clock.set(NOW.plusSeconds(20));
        assertThat(evaluator.evaluate(SCOPE)).isEmpty();
        assertThat(findings.findings(SCOPE)).hasSize(1);

        clock.set(NOW.plusSeconds(30));
        assertThat(evaluator.evaluate(SCOPE)).contains(List.of());
        assertThat(findings.findings(SCOPE)).isEmpty();
    }

    @Test
    void boundsEveryScopeOnItsOwn() {
        Agent prod = agents.register(new AgentRegistration("prod-eu1-a", "0.4.0", SCOPE), NOW.minusSeconds(120));
        Agent dev = agents.register(new AgentRegistration("dev-a", "0.4.0", DEV), NOW.minusSeconds(120));
        snapshots.save(snapshot(prod.id(), NOW.minusSeconds(1), List.of(orders, payments), cycle()));
        snapshots.save(snapshot(dev.id(), DEV, NOW.minusSeconds(1), List.of(orders, payments), cycle()));

        evaluator.onSnapshotIngested(new SnapshotIngested(SCOPE, NOW));
        evaluator.onSnapshotIngested(new SnapshotIngested(DEV, NOW));

        assertThat(findings.findings(SCOPE)).hasSize(1);
        assertThat(findings.findings(DEV)).hasSize(1);
    }

    @Test
    void failedEvaluationIsLoggedAndNeverReachesTheIngestion() {
        assertThatCode(() -> evaluator.onSnapshotIngested(new SnapshotIngested(SCOPE, NOW)))
                .doesNotThrowAnyException();

        assertThat(findings.findings(SCOPE)).isEmpty();
    }

    private List<TopologyEdge> cycle() {
        return List.of(edge(orders, payments, EdgeKind.SYNC, 10), edge(payments, orders, EdgeKind.SYNC, 10));
    }

    private static final class SteppingClock extends Clock {

        private final AtomicReference<Instant> now;

        private SteppingClock(Instant now) {
            this.now = new AtomicReference<>(now);
        }

        private void set(Instant instant) {
            now.set(instant);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }
}
