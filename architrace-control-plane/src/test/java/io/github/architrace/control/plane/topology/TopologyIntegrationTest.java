/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import static io.github.architrace.control.plane.topology.TestTopology.NOW;
import static io.github.architrace.control.plane.topology.TestTopology.SCOPE;
import static io.github.architrace.control.plane.topology.TestTopology.database;
import static io.github.architrace.control.plane.topology.TestTopology.edge;
import static io.github.architrace.control.plane.topology.TestTopology.service;
import static io.github.architrace.control.plane.topology.TestTopology.snapshot;
import static io.github.architrace.control.plane.topology.TestTopology.topic;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.control.plane.PostgresTestcontainers;
import io.github.architrace.control.plane.ingestion.IngestionService;
import io.github.architrace.control.plane.topology.retention.RetentionJob;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;

@SpringBootTest(
        properties = {
            "spring.grpc.server.enabled=false",
            "architrace.topology.retention.period=7d",
            "architrace.topology.retention.batch-size=1"
        })
@Import(PostgresTestcontainers.class)
class TopologyIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClock {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Autowired
    IngestionService ingestion;

    @Autowired
    SnapshotStore snapshots;

    @Autowired
    TopologyQuery query;

    @Autowired
    RetentionJob retention;

    @Autowired
    JdbcClient jdbc;

    @Test
    void twoAgentsFormOneCurrentGraphAndRetentionKeepsTheRecentHistory() {
        Agent a = ingestion.register(new AgentRegistration("it-a", "0.4.0", SCOPE));
        Agent b = ingestion.register(new AgentRegistration("it-b", "0.4.0", SCOPE));
        TopologyNode ordersA = service("orders", "2.8.1", "orders");
        TopologyNode ordersB = service("orders", "2.8.0", "orders-canary");
        TopologyNode db = database("orders");
        TopologyNode events = topic("order-events");
        snapshots.save(snapshot(
                a.id(), NOW.minus(Duration.ofDays(10)), List.of(service("legacy", "1.0", "legacy")), List.of()));
        snapshots.save(snapshot(
                a.id(), NOW.minusSeconds(60), List.of(ordersA, db), List.of(edge(ordersA, db, EdgeKind.SYNC, 100))));
        snapshots.save(snapshot(
                b.id(),
                NOW.minusSeconds(30),
                List.of(ordersB, events),
                List.of(edge(ordersB, events, EdgeKind.PUBLISH, 10))));

        TopologyGraph graph = query.currentGraph(SCOPE, NOW);

        assertThat(graph.nodes())
                .extracting(TopologyNode::id)
                .containsExactly("db:postgresql/orders", "service:orders", "topic:kafka/order-events");
        assertThat(graph.nodes().get(1).attributes().versions()).containsExactlyInAnyOrder("2.8.0", "2.8.1");
        assertThat(graph.edges()).hasSize(2);
        assertThat(query.scopes())
                .filteredOn(summary -> summary.scope().equals(SCOPE))
                .singleElement()
                .isEqualTo(new ScopeSummary(SCOPE, 2, 2, 1, 1, 2, Optional.of(NOW.minusSeconds(30))));

        retention.run();

        Integer remaining = jdbc.sql("select count(*) from snapshot where agent_id in (:a, :b)")
                .param("a", a.id().value())
                .param("b", b.id().value())
                .query(Integer.class)
                .single();
        Integer legacyNodes = jdbc.sql("select count(*) from snapshot_node where node_id = 'service:legacy'")
                .query(Integer.class)
                .single();
        assertThat(remaining).isEqualTo(2);
        assertThat(legacyNodes).isZero();
        assertThat(query.currentGraph(SCOPE, NOW).nodes()).hasSize(3);
    }
}
