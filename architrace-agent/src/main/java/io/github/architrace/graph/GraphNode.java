/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import java.util.Objects;
import java.util.Optional;

public sealed interface GraphNode {

    String id();

    String name();

    record ServiceNode(String domain, String name) implements GraphNode {
        public ServiceNode {
            Objects.requireNonNull(domain, "domain");
            Objects.requireNonNull(name, "name");
        }

        @Override
        public String id() {
            return "service:" + domain + "/" + name;
        }
    }

    record DatabaseNode(String system, Optional<String> namespace) implements GraphNode {
        public DatabaseNode {
            Objects.requireNonNull(system, "system");
            Objects.requireNonNull(namespace, "namespace");
        }

        @Override
        public String id() {
            return "db:" + system + namespace.map(value -> "/" + value).orElse("");
        }

        @Override
        public String name() {
            return namespace.orElse(system);
        }
    }

    record TopicNode(String system, String topic) implements GraphNode {
        public TopicNode {
            Objects.requireNonNull(system, "system");
            Objects.requireNonNull(topic, "topic");
        }

        @Override
        public String id() {
            return "topic:" + system + "/" + topic;
        }

        @Override
        public String name() {
            return topic;
        }
    }

    record ExternalNode(String address) implements GraphNode {
        public ExternalNode {
            Objects.requireNonNull(address, "address");
        }

        @Override
        public String id() {
            return "ext:" + address;
        }

        @Override
        public String name() {
            return address;
        }
    }
}
