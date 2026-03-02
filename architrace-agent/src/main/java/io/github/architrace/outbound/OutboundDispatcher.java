/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.outbound;

import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import java.util.concurrent.ArrayBlockingQueue;

public class OutboundDispatcher {

  private final ArrayBlockingQueue<AgentRegisterRequestedEvent> queue;
  private final OverflowStrategy strategy;

  public OutboundDispatcher(int capacity, OverflowStrategy strategy) {
    this.queue = new ArrayBlockingQueue<>(capacity);
    this.strategy = strategy;
  }

  public void enqueue(AgentRegisterRequestedEvent event) {
    switch (strategy) {
      case DROP_LATEST -> queue.offer(event);
      case DROP_OLDEST -> dropOldestAndPutNew(event);
      case BLOCK -> put(event);
    }
  }

  private void put(AgentRegisterRequestedEvent msg) {
    try {
      queue.put(msg);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void dropOldestAndPutNew(AgentRegisterRequestedEvent msg) {
    if (!queue.offer(msg)) {
      queue.poll();
      queue.offer(msg);
    }
  }

  public AgentRegisterRequestedEvent take() throws InterruptedException {
    return queue.take();
  }
}