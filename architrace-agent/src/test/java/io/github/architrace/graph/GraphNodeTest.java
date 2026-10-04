/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class GraphNodeTest {

    @Test
    void serviceIdCarriesDomainAndNameButNoEnvironment() {
        GraphNode node = new GraphNode.ServiceNode("shop", "checkout");

        assertThat(node.id()).isEqualTo("service:shop/checkout");
        assertThat(node.name()).isEqualTo("checkout");
    }

    @Test
    void databaseIdOmitsTheNamespaceWhenAbsent() {
        GraphNode withNamespace = new GraphNode.DatabaseNode("postgresql", Optional.of("orders"));
        GraphNode withoutNamespace = new GraphNode.DatabaseNode("redis", Optional.empty());

        assertThat(withNamespace.id()).isEqualTo("db:postgresql/orders");
        assertThat(withNamespace.name()).isEqualTo("orders");
        assertThat(withoutNamespace.id()).isEqualTo("db:redis");
        assertThat(withoutNamespace.name()).isEqualTo("redis");
    }

    @Test
    void topicAndExternalIdsFollowTheDecisionRecord() {
        GraphNode topic = new GraphNode.TopicNode("kafka", "orders");
        GraphNode external = new GraphNode.ExternalNode("api.example.com");

        assertThat(topic.id()).isEqualTo("topic:kafka/orders");
        assertThat(topic.name()).isEqualTo("orders");
        assertThat(external.id()).isEqualTo("ext:api.example.com");
        assertThat(external.name()).isEqualTo("api.example.com");
    }
}
