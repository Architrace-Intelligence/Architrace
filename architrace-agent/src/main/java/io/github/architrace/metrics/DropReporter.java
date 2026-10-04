/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.metrics;

import java.util.List;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DropReporter {

    private static final Logger log = LoggerFactory.getLogger(DropReporter.class);

    private final List<Source> sources;
    private final long[] reported;

    public DropReporter(AgentMetrics metrics) {
        this.sources = List.of(
                new Source(
                        "spans rejected by the full queue",
                        () -> metrics.spans().rejected()),
                new Source(
                        "spans ignored from another environment",
                        () -> metrics.graph().foreignSpans()),
                new Source(
                        "spans evicted without a partner", () -> metrics.graph().droppedSpans()),
                new Source(
                        "snapshots dropped by the full queue",
                        () -> metrics.snapshots().dropped()),
                new Source(
                        "snapshots rejected by the control plane",
                        () -> metrics.publisher().rejectedCount()));
        this.reported = new long[sources.size()];
    }

    public Optional<String> report() {
        List<String> changes = IntStream.range(0, sources.size())
                .mapToObj(this::delta)
                .flatMap(Optional::stream)
                .toList();
        if (changes.isEmpty()) {
            return Optional.empty();
        }
        String message = changes.stream().collect(Collectors.joining(", ", "Since the last report: ", ""));
        log.warn(message);
        return Optional.of(message);
    }

    private Optional<String> delta(int index) {
        long current = sources.get(index).value().getAsLong();
        long delta = current - reported[index];
        reported[index] = current;
        return delta > 0 ? Optional.of(delta + " " + sources.get(index).label()) : Optional.empty();
    }

    private record Source(String label, LongSupplier value) {}
}
