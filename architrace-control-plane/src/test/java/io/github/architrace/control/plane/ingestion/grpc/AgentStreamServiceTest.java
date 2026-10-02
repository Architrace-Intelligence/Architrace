/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.ingestion.grpc;

import static io.github.architrace.control.plane.ingestion.SnapshotProtos.WINDOW_END;
import static io.github.architrace.control.plane.ingestion.SnapshotProtos.ordersSnapshot;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.control.plane.ingestion.IngestionMetrics;
import io.github.architrace.control.plane.ingestion.IngestionProperties;
import io.github.architrace.control.plane.ingestion.IngestionService;
import io.github.architrace.control.plane.topology.AgentLiveness;
import io.github.architrace.control.plane.topology.InMemoryAgentStore;
import io.github.architrace.control.plane.topology.InMemorySnapshotStore;
import io.github.architrace.grpc.proto.AgentHealthRequest;
import io.github.architrace.grpc.proto.AgentHealthResponse;
import io.github.architrace.grpc.proto.AgentRegister;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.grpc.proto.GraphBatch;
import io.github.architrace.grpc.proto.GraphSnapshot;
import io.github.architrace.grpc.proto.Heartbeat;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AgentStreamServiceTest {

    private static final Instant NOW = WINDOW_END.plusSeconds(5);

    private final InMemoryAgentStore agents = new InMemoryAgentStore();
    private final InMemorySnapshotStore snapshots = new InMemorySnapshotStore();
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final IngestionMetrics metrics = new IngestionMetrics(registry);
    private final MutableClock clock = new MutableClock(NOW);
    private final IngestionProperties properties =
            new IngestionProperties(Duration.ofSeconds(60), Duration.ofSeconds(30));
    private final AgentStreamService service = new AgentStreamService(
            new IngestionService(agents, snapshots, metrics, clock),
            properties,
            metrics,
            agents,
            new AgentLiveness(properties.heartbeatInterval()),
            clock);

    @Test
    void registrationAnswersWithTheServerIntervals() {
        RecordingObserver<ControlPlaneCommand> responses = new RecordingObserver<>();
        StreamObserver<AgentRegisterRequestedEvent> requests = service.connect(responses);

        requests.onNext(register("prod-eu1-a"));

        assertThat(responses.values).hasSize(1);
        assertThat(responses.values.getFirst().getConfigUpdate().getConfigMap())
                .containsEntry("snapshot.interval-seconds", "60")
                .containsEntry("heartbeat.interval-seconds", "30");
        assertThat(agents.all())
                .singleElement()
                .satisfies(a -> assertThat(a.name()).isEqualTo("prod-eu1-a"));
        assertThat(connected()).isEqualTo(1);
    }

    @Test
    void snapshotIsAcknowledgedWithItsId() {
        RecordingObserver<ControlPlaneCommand> responses = new RecordingObserver<>();
        StreamObserver<AgentRegisterRequestedEvent> requests = service.connect(responses);
        requests.onNext(register("prod-eu1-a"));

        requests.onNext(AgentRegisterRequestedEvent.newBuilder()
                .setSnapshot(ordersSnapshot())
                .build());

        ControlPlaneCommand ack = responses.values.getLast();
        assertThat(ack.hasSnapshotAck()).isTrue();
        assertThat(ack.getSnapshotAck().getWindowEndEpochMs()).isEqualTo(WINDOW_END.toEpochMilli());
        assertThat(snapshots.find(new io.github.architrace.control.plane.topology.SnapshotId(
                        ack.getSnapshotAck().getSnapshotId())))
                .isPresent();
    }

    @Test
    void invalidSnapshotIsRejectedAndTheStreamStaysOpen() {
        RecordingObserver<ControlPlaneCommand> responses = new RecordingObserver<>();
        StreamObserver<AgentRegisterRequestedEvent> requests = service.connect(responses);
        requests.onNext(register("prod-eu1-a"));

        requests.onNext(AgentRegisterRequestedEvent.newBuilder()
                .setSnapshot(GraphSnapshot.newBuilder().setWindowEndEpochMs(10).setWindowStartEpochMs(10))
                .build());
        requests.onNext(AgentRegisterRequestedEvent.newBuilder()
                .setSnapshot(ordersSnapshot())
                .build());

        assertThat(responses.values).hasSize(3);
        assertThat(responses.values.get(1).getSnapshotRejected().getReason()).contains("window end");
        assertThat(responses.values.get(2).hasSnapshotAck()).isTrue();
        assertThat(responses.error).isNull();
        assertThat(registry.counter("architrace.snapshots.rejected").count()).isEqualTo(1.0);
    }

    @Test
    void legacyBatchAndEmptyMessagesAreRejected() {
        RecordingObserver<ControlPlaneCommand> responses = new RecordingObserver<>();
        StreamObserver<AgentRegisterRequestedEvent> requests = service.connect(responses);
        requests.onNext(register("prod-eu1-a"));

        requests.onNext(AgentRegisterRequestedEvent.newBuilder()
                .setGraphBatch(GraphBatch.getDefaultInstance())
                .build());
        requests.onNext(AgentRegisterRequestedEvent.getDefaultInstance());

        assertThat(responses.values).hasSize(3);
        assertThat(responses.values.get(1).getSnapshotRejected().getReason()).contains("deprecated");
        assertThat(responses.values.get(2).getSnapshotRejected().getReason()).contains("empty");
    }

    @Test
    void heartbeatMovesLastSeen() {
        RecordingObserver<ControlPlaneCommand> responses = new RecordingObserver<>();
        StreamObserver<AgentRegisterRequestedEvent> requests = service.connect(responses);
        requests.onNext(register("prod-eu1-a"));
        clock.advance(Duration.ofSeconds(20));

        requests.onNext(AgentRegisterRequestedEvent.newBuilder()
                .setHeartbeat(Heartbeat.getDefaultInstance())
                .build());

        assertThat(agents.all().getFirst().lastSeenAt()).isEqualTo(NOW.plusSeconds(20));
    }

    @Test
    void snapshotBeforeRegistrationFailsTheStream() {
        RecordingObserver<ControlPlaneCommand> responses = new RecordingObserver<>();
        StreamObserver<AgentRegisterRequestedEvent> requests = service.connect(responses);

        requests.onNext(AgentRegisterRequestedEvent.newBuilder()
                .setSnapshot(ordersSnapshot())
                .build());
        requests.onNext(register("late"));

        assertThat(responses.error).isInstanceOf(StatusRuntimeException.class);
        assertThat(Status.fromThrowable(responses.error).getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
        assertThat(agents.all()).isEmpty();
    }

    @Test
    void blankScopeAndDoubleRegistrationAreInvalid() {
        RecordingObserver<ControlPlaneCommand> blank = new RecordingObserver<>();
        service.connect(blank)
                .onNext(AgentRegisterRequestedEvent.newBuilder()
                        .setRegister(AgentRegister.newBuilder().setAgentName("x"))
                        .build());
        assertThat(Status.fromThrowable(blank.error).getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);

        RecordingObserver<ControlPlaneCommand> twice = new RecordingObserver<>();
        StreamObserver<AgentRegisterRequestedEvent> requests = service.connect(twice);
        requests.onNext(register("prod-eu1-a"));
        requests.onNext(register("prod-eu1-a"));
        assertThat(Status.fromThrowable(twice.error).getCode()).isEqualTo(Status.Code.FAILED_PRECONDITION);
        assertThat(connected()).isZero();
    }

    @Test
    void completionAndErrorsReleaseTheConnection() {
        RecordingObserver<ControlPlaneCommand> completed = new RecordingObserver<>();
        StreamObserver<AgentRegisterRequestedEvent> first = service.connect(completed);
        first.onNext(register("a"));
        first.onCompleted();
        assertThat(completed.completed).isTrue();

        RecordingObserver<ControlPlaneCommand> failed = new RecordingObserver<>();
        StreamObserver<AgentRegisterRequestedEvent> second = service.connect(failed);
        second.onNext(register("b"));
        second.onError(new IllegalStateException("gone"));
        second.onError(new IllegalStateException("gone again"));
        assertThat(failed.completed).isFalse();
        assertThat(connected()).isZero();
    }

    @Test
    void healthReflectsLivenessOfTheLatestAgentWithThatName() {
        service.connect(new RecordingObserver<>()).onNext(register("prod-eu1-a"));
        clock.advance(Duration.ofSeconds(100));

        assertThat(health("prod-eu1-a").getLive()).isFalse();
        assertThat(health("prod-eu1-a").getLastSeenEpochMs()).isEqualTo(NOW.toEpochMilli());
        assertThat(health("unknown").getLive()).isFalse();
        assertThat(health("unknown").getLastSeenEpochMs()).isZero();

        service.connect(new RecordingObserver<>())
                .onNext(AgentRegisterRequestedEvent.newBuilder()
                        .setHeartbeat(Heartbeat.getDefaultInstance())
                        .build());
        service.connect(new RecordingObserver<>()).onNext(register("prod-eu1-a"));
        assertThat(health("prod-eu1-a").getLive()).isTrue();
    }

    private AgentHealthResponse health(String name) {
        RecordingObserver<AgentHealthResponse> observer = new RecordingObserver<>();
        service.getAgentHealth(
                AgentHealthRequest.newBuilder().setAgentName(name).build(), observer);
        assertThat(observer.completed).isTrue();
        return observer.values.getFirst();
    }

    private int connected() {
        return (int) registry.get("architrace.agents.connected").gauge().value();
    }

    private static AgentRegisterRequestedEvent register(String name) {
        return AgentRegisterRequestedEvent.newBuilder()
                .setRegister(AgentRegister.newBuilder()
                        .setAgentName(name)
                        .setAgentVersion("0.4.0")
                        .setProject("webshop")
                        .setEnvironment("PROD")
                        .setClusterId("k8s-prod-eu1"))
                .build();
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
