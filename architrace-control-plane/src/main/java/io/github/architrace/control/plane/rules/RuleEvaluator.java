/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import io.github.architrace.control.plane.ingestion.SnapshotIngested;
import io.github.architrace.control.plane.topology.Scope;
import io.github.architrace.control.plane.topology.TopologyGraph;
import io.github.architrace.control.plane.topology.TopologyQuery;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class RuleEvaluator {

    private static final Logger log = LoggerFactory.getLogger(RuleEvaluator.class);

    private final TopologyQuery topology;
    private final RuleEngine engine;
    private final FindingStore findings;
    private final Duration interval;
    private final Clock clock;
    private final ConcurrentMap<Scope, Instant> evaluatedAt = new ConcurrentHashMap<>();

    public RuleEvaluator(
            TopologyQuery topology, RuleEngine engine, FindingStore findings, RulesProperties properties, Clock clock) {
        this.topology = topology;
        this.engine = engine;
        this.findings = findings;
        this.interval = properties.evaluationInterval();
        this.clock = clock;
    }

    @EventListener
    public void onSnapshotIngested(SnapshotIngested event) {
        try {
            evaluate(event.scope());
        } catch (RuntimeException e) {
            log.error("Rule evaluation failed for {}", event.scope(), e);
        }
    }

    public Optional<List<Finding>> evaluate(Scope scope) {
        Instant now = clock.instant();
        if (!claim(scope, now)) {
            return Optional.empty();
        }
        TopologyGraph graph = topology.currentGraph(scope, now);
        List<Finding> result = engine.evaluate(graph);
        findings.replace(scope, result);
        log.info("Evaluated {} rules for {}: {} findings", engine.rules().size(), scope, result.size());
        return Optional.of(result);
    }

    private boolean claim(Scope scope, Instant now) {
        Instant last = evaluatedAt.get(scope);
        if (last == null) {
            return evaluatedAt.putIfAbsent(scope, now) == null;
        }
        return !now.isBefore(last.plus(interval)) && evaluatedAt.replace(scope, last, now);
    }
}
