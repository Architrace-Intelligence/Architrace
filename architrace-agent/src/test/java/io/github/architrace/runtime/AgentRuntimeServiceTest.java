/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.runtime;


import io.github.architrace.controlplane.ControlPlaneBootstrapService;
import io.github.architrace.controlplane.ControlPlaneLifecycle;
import io.github.architrace.controlplane.RegistrationService;
import io.github.architrace.core.config.AgentConfig;
import io.github.architrace.core.config.AgentConfigLoader;
import io.github.architrace.grpc.TransportClient;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.testsupport.TestDataProvider;
import io.grpc.stub.StreamObserver;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

class AgentRuntimeServiceTest {

  @TempDir
  Path tempDir;

  @Test
  void runShouldFailFastWhenConfigDoesNotExist() {
    AgentRuntimeService sut = new AgentRuntimeService(new AgentConfigLoader(), null);
    Path missingConfig = tempDir.resolve("missing-config.yaml");

    assertThatThrownBy(() -> sut.run(missingConfig))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void runControlPlaneSupervisorShouldStopWhenSessionFails() throws Exception {
    AtomicBoolean lifecycleClosed = new AtomicBoolean(false);
    ControlPlaneLifecycle failingLifecycle = new ControlPlaneLifecycle(
        "agent-a",
        new ThrowingTransportClient(lifecycleClosed),
        new RegistrationService(),
        List.of());

    ControlPlaneBootstrapService bootstrapService = new ControlPlaneBootstrapService(null) {
      @Override
      public ControlPlaneLifecycle bootstrap(AgentConfig config) {
        return failingLifecycle;
      }
    };
    AgentRuntimeService sut = new AgentRuntimeService(new AgentConfigLoader(), bootstrapService);

    invokePrivate(
        sut,
        "runControlPlaneSupervisor",
        new Class[] {AgentConfig.class},
        validConfig(1L));

    assertThat(Thread.currentThread().isInterrupted()).isTrue();
    Thread.interrupted();
    assertThat(lifecycleClosed.get()).isTrue();
  }

  @Test
  void sleepBeforeRetryShouldReturnFalseWhenThreadInterrupted() throws Exception {
    AgentRuntimeService sut = new AgentRuntimeService(new AgentConfigLoader(), null);
    Thread.currentThread().interrupt();

    Object result = invokePrivate(sut, "sleepBeforeRetry", new Class[] {Long.class}, 1L);

    assertThat(result).isEqualTo(false);
    assertThat(Thread.currentThread().isInterrupted()).isTrue();
    Thread.interrupted();
  }

  @Test
  void runReceiverShouldStopWhenInterrupted() throws Exception {
    AgentRuntimeService sut = new AgentRuntimeService(new AgentConfigLoader(), null);
    int freePort = TestDataProvider.findFreePort();
    AgentConfig config =
        new AgentConfig(
            "cluster-1",
            new AgentConfig.Agent("agent-a"),
            new AgentConfig.ControlPlane(new AgentConfig.Bootstrap("localhost:9090")),
            freePort,
            1L);

    AtomicReference<Throwable> failure = new AtomicReference<>();
    Thread worker = Thread.ofVirtual().start(() -> {
      try {
        invokePrivate(sut, "runReceiver", new Class[] {AgentConfig.class}, config);
      } catch (Throwable throwable) {
        failure.set(throwable);
      }
    });

    Thread.sleep(200);
    worker.interrupt();
    worker.join(2_000);

    assertThat(failure.get()).isInstanceOf(InterruptedException.class);
  }

  private static AgentConfig validConfig(Long retrySeconds) {
    return new AgentConfig(
        "cluster-1",
        new AgentConfig.Agent("agent-a"),
        new AgentConfig.ControlPlane(new AgentConfig.Bootstrap("localhost:9090")),
        4319,
        retrySeconds);
  }

  private static Object invokePrivate(Object target, String methodName, Class<?>[] signature, Object... args)
      throws Exception {
    Method method = target.getClass().getDeclaredMethod(methodName, signature);
    method.setAccessible(true);
    try {
      return method.invoke(target, args);
    } catch (InvocationTargetException ex) {
      Throwable cause = ex.getCause();
      if (cause instanceof Exception exception) {
        throw exception;
      }
      throw ex;
    }
  }

  private static final class ThrowingTransportClient implements TransportClient {
    private final AtomicBoolean closeCalled;

    private ThrowingTransportClient(AtomicBoolean closeCalled) {
      this.closeCalled = closeCalled;
    }

    @Override
    public StreamObserver<AgentRegisterRequestedEvent> open(StreamObserver<ControlPlaneCommand> inboundObserver) {
      throw new IllegalStateException("boom");
    }

    @Override
    public void close() {
      closeCalled.set(true);
    }
  }
}
