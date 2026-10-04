/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.drift;

import static io.github.architrace.control.plane.topology.TestTopology.NOW;
import static io.github.architrace.control.plane.topology.TestTopology.SCOPE;
import static io.github.architrace.control.plane.topology.TestTopology.database;
import static io.github.architrace.control.plane.topology.TestTopology.edge;
import static io.github.architrace.control.plane.topology.TestTopology.service;
import static io.github.architrace.control.plane.topology.TestTopology.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.github.architrace.control.plane.topology.Agent;
import io.github.architrace.control.plane.topology.AgentLiveness;
import io.github.architrace.control.plane.topology.AgentRegistration;
import io.github.architrace.control.plane.topology.EdgeKey;
import io.github.architrace.control.plane.topology.EdgeKind;
import io.github.architrace.control.plane.topology.InMemoryAgentStore;
import io.github.architrace.control.plane.topology.InMemorySnapshotStore;
import io.github.architrace.control.plane.topology.InvalidQueryException;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.ScopeNotFoundException;
import io.github.architrace.control.plane.topology.TopologyMetrics;
import io.github.architrace.control.plane.topology.TopologyNode;
import io.github.architrace.control.plane.topology.TopologyQuery;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class DriftQueryTest {

    private static final Scope DEV = new Scope("webshop", "DEV", "k8s-dev");
    private static final Instant YESTERDAY = NOW.minusSeconds(86_400);

    private final InMemoryAgentStore agents = new InMemoryAgentStore();
    private final InMemorySnapshotStore snapshots = new InMemorySnapshotStore();
    private final DriftQuery query = new DriftQuery(new TopologyQuery(
            agents,
            snapshots,
            new AgentLiveness(Duration.ofSeconds(30)),
            new TopologyMetrics(new SimpleMeterRegistry()),
            Clock.fixed(NOW, ZoneOffset.UTC)));

    @Test
    void environmentDiffComparesTheCurrentGraphsOfTwoScopesAtTheSameInstant() {
        Agent dev = register("dev", DEV);
        Agent prod = register("prod", SCOPE);
        TopologyNode devOrders = service("orders", "2.9.0", "orders");
        TopologyNode prodOrders = service("orders", "2.8.1", "orders");
        TopologyNode db = database("orders");
        TopologyNode search = service("search", "1.0.0", "search");
        snapshots.save(snapshot(dev.id(), DEV, NOW.minusSeconds(60), List.of(devOrders, db, search), List.of()));
        snapshots.save(snapshot(
                prod.id(),
                NOW.minusSeconds(60),
                List.of(prodOrders, db),
                List.of(edge(prodOrders, db, EdgeKind.SYNC, 1))));
        snapshots.save(snapshot(prod.id(), NOW.plusSeconds(60), List.of(service("future", "1", "ns")), List.of()));

        TopologyDiff diff = query.environments(DEV, SCOPE, NOW);

        assertThat(diff.left()).isEqualTo(new GraphRef(DEV, NOW));
        assertThat(diff.right()).isEqualTo(new GraphRef(SCOPE, NOW));
        assertThat(diff.nodesAdded()).isEmpty();
        assertThat(diff.nodesRemoved()).containsExactly(search);
        assertThat(diff.nodesChanged()).containsExactly(new NodeChange(devOrders, prodOrders));
        assertThat(diff.edgesAdded()).containsExactly(new EdgeKey(prodOrders.id(), db.id(), EdgeKind.SYNC));
        assertThat(diff.edgesRemoved()).isEmpty();
    }

    @Test
    void timelineDiffComparesOneScopeAtTwoInstants() {
        Agent prod = register("prod", SCOPE);
        TopologyNode before = service("orders", "2.8.0", "orders");
        TopologyNode after = service("orders", "2.8.1", "orders");
        snapshots.save(snapshot(prod.id(), YESTERDAY, List.of(before), List.of()));
        snapshots.save(snapshot(prod.id(), NOW, List.of(after), List.of()));

        TopologyDiff diff = query.timeline(SCOPE, YESTERDAY, NOW);

        assertThat(diff.left()).isEqualTo(new GraphRef(SCOPE, YESTERDAY));
        assertThat(diff.right()).isEqualTo(new GraphRef(SCOPE, NOW));
        assertThat(diff.nodesChanged()).containsExactly(new NodeChange(before, after));
    }

    @Test
    void timelineDiffRejectsFromAfterTo() {
        register("prod", SCOPE);
        Instant from = NOW;
        Instant to = YESTERDAY;

        assertThatExceptionOfType(InvalidQueryException.class)
                .isThrownBy(() -> query.timeline(SCOPE, from, to))
                .withMessage("from 2026-10-01T12:00:00Z must not be after to 2026-09-30T12:00:00Z");
    }

    @Test
    void eitherSideWithoutAnAgentIsNotFound() {
        register("prod", SCOPE);

        assertThatExceptionOfType(ScopeNotFoundException.class)
                .isThrownBy(() -> query.environments(DEV, SCOPE, NOW))
                .withMessageContaining("webshop/DEV/k8s-dev");
        assertThatExceptionOfType(ScopeNotFoundException.class)
                .isThrownBy(() -> query.environments(SCOPE, DEV, NOW))
                .withMessageContaining("webshop/DEV/k8s-dev");
    }

    private Agent register(String name, Scope scope) {
        return agents.register(new AgentRegistration(name, "0.4.0", scope), NOW);
    }
}
