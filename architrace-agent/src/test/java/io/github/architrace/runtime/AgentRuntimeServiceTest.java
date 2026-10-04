/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.github.architrace.controlplane.ControlPlaneClientFactory;
import io.github.architrace.core.config.AgentConfig;
import io.github.architrace.grpc.TransportClient;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent.PayloadCase;
import io.github.architrace.grpc.proto.GraphSnapshot;
import io.github.architrace.grpc.proto.SnapshotEdge;
import io.github.architrace.service.runtime.AgentRuntimeService;
import io.github.architrace.testsupport.OtlpRequests;
import io.github.architrace.testsupport.StubControlPlane;
import io.github.architrace.testsupport.TestDataProvider;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc;
import io.opentelemetry.proto.trace.v1.Span;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AgentRuntimeServiceTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private StubControlPlane controlPlane;
    private Thread agent;

    @AfterEach
    void stop() throws InterruptedException {
        if (agent != null) {
            agent.interrupt();
            agent.join(TIMEOUT.toMillis());
        }
        if (controlPlane != null) {
            controlPlane.close();
        }
    }

    @Test
    void tracesInBecomeAcknowledgedSnapshotsOutAndEverythingStopsOnInterrupt() throws Exception {
        controlPlane = StubControlPlane.start(Duration.ofSeconds(30));
        int otlpPort = TestDataProvider.findFreePort();
        AgentConfig config =
                TestDataProvider.agentConfig("localhost:1", otlpPort, Duration.ofMillis(100), Duration.ofSeconds(1));
        AgentRuntimeService sut = new AgentRuntimeService(inProcessFactory());
        AtomicReference<Throwable> outcome = new AtomicReference<>();
        agent = Thread.ofPlatform().start(() -> {
            try {
                sut.run(config);
            } catch (Throwable throwable) {
                outcome.set(throwable);
            }
        });
        await().atMost(TIMEOUT).until(() -> isListening(otlpPort));
        await().atMost(TIMEOUT).until(() -> controlPlane.count(PayloadCase.REGISTER) == 1);

        exportTraces(otlpPort);

        await().atMost(TIMEOUT).until(() -> controlPlane.snapshots().stream().anyMatch(s -> s.getEdgesCount() > 0));
        GraphSnapshot snapshot = controlPlane.snapshots().stream()
                .filter(s -> s.getEdgesCount() > 0)
                .findFirst()
                .orElseThrow();
        assertThat(snapshot.getEdgesList())
                .extracting(SnapshotEdge::getSourceId, SnapshotEdge::getTargetId)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("service:shop/checkout", "service:shop/orders"));
        assertThat(snapshot.getEdges(0).getMetrics().getCalls()).isEqualTo(1);
        assertThat(snapshot.getWindowEndEpochMs()).isGreaterThan(snapshot.getWindowStartEpochMs());

        agent.interrupt();
        agent.join(TIMEOUT.toMillis());

        assertThat(agent.isAlive()).isFalse();
        assertThat(outcome.get()).isInstanceOf(InterruptedException.class);
        await().atMost(TIMEOUT).until(() -> !isListening(otlpPort));
    }

    private ControlPlaneClientFactory inProcessFactory() {
        return new ControlPlaneClientFactory() {
            @Override
            public TransportClient create(String server) {
                return controlPlane.transport();
            }
        };
    }

    private static void exportTraces(int otlpPort) throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", otlpPort)
                .usePlaintext()
                .build();
        try {
            TraceServiceGrpc.TraceServiceBlockingStub client = TraceServiceGrpc.newBlockingStub(channel);
            client.export(OtlpRequests.request(
                    OtlpRequests.standardResource("checkout"),
                    OtlpRequests.span(
                                    Span.SpanKind.SPAN_KIND_CLIENT, OtlpRequests.TRACE_ID, OtlpRequests.CLIENT_SPAN_ID)
                            .addAttributes(OtlpRequests.text("server.address", "orders"))));
            client.export(OtlpRequests.request(
                    List.of(
                            OtlpRequests.text("service.name", "orders"),
                            OtlpRequests.text("service.namespace", "shop"),
                            OtlpRequests.text("deployment.environment.name", "PROD")),
                    OtlpRequests.childSpan(
                            Span.SpanKind.SPAN_KIND_SERVER,
                            OtlpRequests.TRACE_ID,
                            OtlpRequests.SERVER_SPAN_ID,
                            OtlpRequests.CLIENT_SPAN_ID)));
        } finally {
            channel.shutdownNow();
            channel.awaitTermination(2, TimeUnit.SECONDS);
        }
    }

    private static boolean isListening(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 200);
            return true;
        } catch (IOException _) {
            return false;
        }
    }
}
