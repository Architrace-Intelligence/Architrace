/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.controlplane;

import io.github.architrace.publish.PublisherStats;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ControlPlaneSupervisor {

    private static final Logger log = LoggerFactory.getLogger(ControlPlaneSupervisor.class);

    private final Supplier<ControlPlaneSession> sessions;
    private final Duration retryDelay;
    private final PublisherStats stats;

    public ControlPlaneSupervisor(Supplier<ControlPlaneSession> sessions, Duration retryDelay, PublisherStats stats) {
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.retryDelay = Objects.requireNonNull(retryDelay, "retryDelay");
        this.stats = Objects.requireNonNull(stats, "stats");
    }

    public Void run() throws InterruptedException {
        while (!Thread.currentThread().isInterrupted()) {
            runOnce();
            stats.sessionEnded();
            Thread.sleep(retryDelay);
        }
        return null;
    }

    private void runOnce() throws InterruptedException {
        try {
            sessions.get().run();
            log.info("Control plane session ended; reconnecting in {}", retryDelay);
        } catch (RuntimeException e) {
            log.warn("Control plane session failed ({}); reconnecting in {}", describe(e), retryDelay);
        }
    }

    private static String describe(Throwable throwable) {
        Throwable cause = throwable.getCause() == null ? throwable : throwable.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
