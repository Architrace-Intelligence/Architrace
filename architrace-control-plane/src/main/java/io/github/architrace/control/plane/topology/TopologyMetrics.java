/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class TopologyMetrics {

    private final Timer graphQueries;
    private final Counter deleted;

    public TopologyMetrics(MeterRegistry registry) {
        this.graphQueries = registry.timer("architrace.topology.query");
        this.deleted = registry.counter("architrace.snapshots.deleted");
    }

    public <T> T recordGraphQuery(Supplier<T> query) {
        return graphQueries.record(query);
    }

    public void snapshotsDeleted(int count) {
        deleted.increment(count);
    }
}
