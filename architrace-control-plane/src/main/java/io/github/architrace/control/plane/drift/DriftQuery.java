/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.drift;

import io.github.architrace.control.plane.topology.InvalidQueryException;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.TopologyQuery;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class DriftQuery {

    private final TopologyQuery topology;

    public DriftQuery(TopologyQuery topology) {
        this.topology = topology;
    }

    public TopologyDiff environments(Scope left, Scope right, Instant at) {
        return GraphDiffer.diff(
                topology.currentGraph(left, at), topology.currentGraph(right, at), DiffMode.ENVIRONMENTS);
    }

    public TopologyDiff timeline(Scope scope, Instant from, Instant to) {
        if (from.isAfter(to)) {
            throw new InvalidQueryException("from " + from + " must not be after to " + to);
        }
        return GraphDiffer.diff(
                topology.currentGraph(scope, from), topology.currentGraph(scope, to), DiffMode.TIMELINE);
    }
}
