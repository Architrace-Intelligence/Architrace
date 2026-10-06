/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public final class LatencyHistogram {

    private static final int BUCKETS = 27;
    private static final long[] UPPER_BOUNDS_MICROS =
            LongStream.iterate(1L, bound -> bound * 2).limit(BUCKETS).toArray();

    private final long[] counts = new long[BUCKETS + 1];
    private long total;
    private long maxMicros;

    public void observe(long latencyMicros) {
        counts[bucketOf(latencyMicros)]++;
        total++;
        maxMicros = Math.max(maxMicros, latencyMicros);
    }

    public long total() {
        return total;
    }

    public long maxMicros() {
        return maxMicros;
    }

    public long percentileMicros(double percentile) {
        if (total == 0) {
            return 0;
        }
        long target = (long) Math.ceil(percentile * total);
        long[] cumulative = Arrays.copyOf(counts, counts.length);
        Arrays.parallelPrefix(cumulative, Long::sum);
        int bucket = IntStream.range(0, cumulative.length)
                .filter(index -> cumulative[index] >= target)
                .findFirst()
                .orElse(BUCKETS);
        return bucket < BUCKETS ? Math.min(UPPER_BOUNDS_MICROS[bucket], maxMicros) : maxMicros;
    }

    private static int bucketOf(long latencyMicros) {
        int bucket = latencyMicros <= 1 ? 0 : Long.SIZE - Long.numberOfLeadingZeros(latencyMicros - 1);
        return Math.min(bucket, BUCKETS);
    }
}
