/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public final class LatencyHistogram {

    private static final int BUCKETS = 17;
    private static final long[] UPPER_BOUNDS_MILLIS =
            LongStream.iterate(1L, bound -> bound * 2).limit(BUCKETS).toArray();

    private final long[] counts = new long[BUCKETS + 1];
    private long total;
    private long maxMillis;

    public void record(long latencyMillis) {
        counts[bucketOf(latencyMillis)]++;
        total++;
        maxMillis = Math.max(maxMillis, latencyMillis);
    }

    public long total() {
        return total;
    }

    public long maxMillis() {
        return maxMillis;
    }

    public long percentileMillis(double percentile) {
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
        return bucket < BUCKETS ? Math.min(UPPER_BOUNDS_MILLIS[bucket], maxMillis) : maxMillis;
    }

    private static int bucketOf(long latencyMillis) {
        return IntStream.range(0, BUCKETS)
                .filter(index -> latencyMillis <= UPPER_BOUNDS_MILLIS[index])
                .findFirst()
                .orElse(BUCKETS);
    }
}
