/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.controlplane;


import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.grpc.stub.StreamObserver;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ControlPlaneRegistryTest {

  private static final String AGENT_NAME = "agent-a";
  private static final String UNKNOWN_AGENT = "unknown";
  private static final long HEALTH_LIVE_THRESHOLD_MS = 15_000L;
  private static final long SHORT_LIVE_THRESHOLD_MS = 1_000L;
  private static final int FIRST_EVENT_INDEX = 0;
  private static final String INITIAL_CONFIG_VERSION = "1";
  private static final long EXPECTED_EMPTY_LAST_SEEN = 0L;
  private static final long MIN_CONFIG_EVENTS = 2L;

  @InjectMocks
  private ControlPlaneRegistry sut;

  @Test
  void registerShouldSendInitialConfigAndHealthShouldBeLive() {
    RecordingObserver<ControlPlaneCommand> responseObserver = new RecordingObserver<>();

    sut.register(AGENT_NAME, responseObserver);

    List<ControlPlaneCommand> events = responseObserver.values;
    ControlPlaneCommand firstEvent = events.get(FIRST_EVENT_INDEX);
    assertThat(firstEvent.hasConfigUpdate()).isTrue();
    assertThat(firstEvent.getConfigUpdate().getVersion()).isEqualTo(INITIAL_CONFIG_VERSION);
    assertThat(firstEvent.getConfigUpdate().getConfigOrThrow("agent.mode")).isEqualTo("managed");
    assertThat(firstEvent.getConfigUpdate().getConfigOrThrow("agent.bootstrap")).isEqualTo("done");

    ControlPlaneRegistry.HealthState health = sut.health(AGENT_NAME, HEALTH_LIVE_THRESHOLD_MS);
    assertThat(health.live()).isTrue();
    assertThat(health.lastSeenEpochMs()).isPositive();
  }

  @SuppressWarnings("unchecked")
  @Test
  void tickShouldSendPeriodicConfigUpdate() {
    RecordingObserver<ControlPlaneCommand> responseObserver = new RecordingObserver<>();
    sut.register(AGENT_NAME, responseObserver);

    sut.tick();
    sut.tick();
    sut.tick();

    long configUpdateCount =
        responseObserver.values.stream().filter(ControlPlaneCommand::hasConfigUpdate).count();

    assertThat(configUpdateCount).isGreaterThanOrEqualTo(MIN_CONFIG_EVENTS);
  }

  @Test
  void touchShouldUpdateLastSeenAndUnregisterShouldRemoveSession() {
    RecordingObserver<ControlPlaneCommand> responseObserver = new RecordingObserver<>();
    sut.register(AGENT_NAME, responseObserver);

    long lastSeenBeforeTouch = sut.health(AGENT_NAME, HEALTH_LIVE_THRESHOLD_MS).lastSeenEpochMs();
    sut.touch(AGENT_NAME);

    ControlPlaneRegistry.HealthState healthBeforeUnregister = sut.health(AGENT_NAME, HEALTH_LIVE_THRESHOLD_MS);
    assertThat(healthBeforeUnregister.live()).isTrue();
    assertThat(healthBeforeUnregister.lastSeenEpochMs()).isGreaterThanOrEqualTo(lastSeenBeforeTouch);

    sut.unregister(AGENT_NAME);
    ControlPlaneRegistry.HealthState healthAfterUnregister = sut.health(AGENT_NAME, HEALTH_LIVE_THRESHOLD_MS);
    assertThat(healthAfterUnregister.live()).isFalse();
    assertThat(healthAfterUnregister.lastSeenEpochMs()).isEqualTo(EXPECTED_EMPTY_LAST_SEEN);

    sut.unregister(null);
  }

  @Test
  void healthShouldBeNotLiveForUnknownAgent() {
    ControlPlaneRegistry.HealthState health = sut.health(UNKNOWN_AGENT, SHORT_LIVE_THRESHOLD_MS);

    assertThat(health.live()).isFalse();
    assertThat(health.lastSeenEpochMs()).isEqualTo(EXPECTED_EMPTY_LAST_SEEN);
  }

  private static final class RecordingObserver<T> implements StreamObserver<T> {
    private final List<T> values = new ArrayList<>();

    @Override
    public void onNext(T value) {
      values.add(value);
    }

    @Override
    public void onError(Throwable throwable) {
      // No-op for tests; events are asserted via recorded onNext values.
    }

    @Override
    public void onCompleted() {
      // No-op for tests; completion state is not relevant for registry assertions.
    }
  }
}
