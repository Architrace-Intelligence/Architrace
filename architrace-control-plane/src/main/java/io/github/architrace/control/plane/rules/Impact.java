/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import io.github.architrace.control.plane.topology.TopologyNode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record Impact(
        TopologyNode subject,
        Instant at,
        List<ImpactedNode> impaired,
        List<ImpactedNode> delayed,
        int services,
        int servicesTotal) {

    public Impact {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(at, "at");
        impaired = List.copyOf(impaired);
        delayed = List.copyOf(delayed);
        if (services < 0 || servicesTotal < services) {
            throw new IllegalArgumentException("services must be between 0 and servicesTotal");
        }
    }

    public record ImpactedNode(TopologyNode node, int distance, List<String> path) {

        public ImpactedNode {
            Objects.requireNonNull(node, "node");
            path = List.copyOf(path);
            if (distance < 1 || path.size() != distance + 1 || !path.getFirst().equals(node.id())) {
                throw new IllegalArgumentException("path must lead from the node to the subject in distance hops");
            }
        }
    }
}
