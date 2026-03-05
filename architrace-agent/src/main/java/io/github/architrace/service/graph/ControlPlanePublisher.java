/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.service.graph;

import io.github.architrace.controlplane.ControlPlaneLifecycle;
import io.github.architrace.grpc.proto.GraphBatch;
import java.util.Objects;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlPlanePublisher {

  private static final Logger log = LoggerFactory.getLogger(ControlPlanePublisher.class);
  private final Supplier<ControlPlaneLifecycle> lifecycleSupplier;

  public ControlPlanePublisher(Supplier<ControlPlaneLifecycle> lifecycleSupplier) {
    this.lifecycleSupplier = Objects.requireNonNull(lifecycleSupplier, "lifecycleSupplier");
  }

  public void publish(GraphBatch batch) {
    ControlPlaneLifecycle lifecycle = lifecycleSupplier.get();
    if (lifecycle == null) {
      log.debug("Control-plane lifecycle is not active; graph batch skipped.");
      return;
    }

    lifecycle.publishGraphBatch(batch);
  }
}

