/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.ingestion.grpc;

import static io.github.architrace.control.plane.ingestion.SnapshotProtos.ordersSnapshot;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.control.plane.PostgresTestcontainers;
import io.github.architrace.control.plane.topology.AgentStore;
import io.github.architrace.control.plane.topology.Snapshot;
import io.github.architrace.control.plane.topology.SnapshotId;
import io.github.architrace.control.plane.topology.SnapshotStore;
import io.github.architrace.grpc.proto.AgentRegister;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.grpc.proto.ControlPlaneServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = "spring.grpc.server.enabled=false")
@Import(PostgresTestcontainers.class)
class AgentStreamIntegrationTest {

  @Autowired AgentStreamService service;
  @Autowired AgentStore agents;
  @Autowired SnapshotStore snapshots;

  private Server server;
  private ManagedChannel channel;

  @BeforeEach
  void startInProcessServer() throws IOException {
    String name = InProcessServerBuilder.generateName();
    server = InProcessServerBuilder.forName(name).directExecutor().addService(service).build().start();
    channel = InProcessChannelBuilder.forName(name).directExecutor().build();
  }

  @AfterEach
  void stop() {
    channel.shutdownNow();
    server.shutdownNow();
  }

  @Test
  void agentRegistersAndItsSnapshotLandsInPostgres() throws InterruptedException {
    List<ControlPlaneCommand> commands = new CopyOnWriteArrayList<>();
    CountDownLatch done = new CountDownLatch(1);
    StreamObserver<AgentRegisterRequestedEvent> requests =
        ControlPlaneServiceGrpc.newStub(channel)
            .connect(
                new StreamObserver<>() {
                  @Override
                  public void onNext(ControlPlaneCommand value) {
                    commands.add(value);
                  }

                  @Override
                  public void onError(Throwable t) {
                    done.countDown();
                  }

                  @Override
                  public void onCompleted() {
                    done.countDown();
                  }
                });

    requests.onNext(
        AgentRegisterRequestedEvent.newBuilder()
            .setRegister(
                AgentRegister.newBuilder()
                    .setAgentName("it-agent")
                    .setAgentVersion("0.4.0")
                    .setProject("webshop")
                    .setEnvironment("PROD")
                    .setClusterId("k8s-prod-eu1"))
            .build());
    requests.onNext(AgentRegisterRequestedEvent.newBuilder().setSnapshot(ordersSnapshot()).build());
    requests.onCompleted();

    assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
    assertThat(commands).hasSize(2);
    assertThat(commands.getFirst().hasConfigUpdate()).isTrue();
    long snapshotId = commands.get(1).getSnapshotAck().getSnapshotId();
    Snapshot stored = snapshots.find(new SnapshotId(snapshotId)).orElseThrow();
    assertThat(stored.nodes()).hasSize(3);
    assertThat(stored.edges()).hasSize(2);
    assertThat(agents.all()).anySatisfy(a -> assertThat(a.name()).isEqualTo("it-agent"));
  }
}
