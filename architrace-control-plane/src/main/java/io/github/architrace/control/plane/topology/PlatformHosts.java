/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public record PlatformHosts(Set<String> hosts) {

    public static final String CATEGORY_LABEL = "category";
    public static final String PLATFORM = "platform";

    private static final PlatformHosts NONE = new PlatformHosts(Set.of());

    public PlatformHosts {
        hosts = Set.copyOf(hosts);
    }

    public static PlatformHosts none() {
        return NONE;
    }

    public static boolean isPlatform(TopologyNode node) {
        return PLATFORM.equals(node.attributes().labels().get(CATEGORY_LABEL));
    }

    public TopologyGraph classify(TopologyGraph graph) {
        if (hosts.isEmpty()) {
            return graph;
        }
        return new TopologyGraph(
                graph.scope(),
                graph.at(),
                graph.nodes().stream().map(this::classify).toList(),
                graph.edges());
    }

    private TopologyNode classify(TopologyNode node) {
        if (node.type() != NodeType.EXTERNAL || !hosts.contains(node.name())) {
            return node;
        }
        Map<String, String> labels = new HashMap<>(node.attributes().labels());
        labels.put(CATEGORY_LABEL, PLATFORM);
        NodeAttributes attributes = node.attributes();
        return new TopologyNode(
                node.id(),
                node.type(),
                node.name(),
                new NodeAttributes(attributes.versions(), attributes.deployments(), labels));
    }
}
