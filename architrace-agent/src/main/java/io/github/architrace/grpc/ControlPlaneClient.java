/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.grpc;

import com.google.inject.Inject;
import io.github.architrace.grpc.proto.AgentRegisterRequestedEvent;
import io.github.architrace.grpc.proto.ControlPlaneCommand;
import io.github.architrace.grpc.proto.ControlPlaneServiceGrpc;
import io.github.architrace.otlp.GraphNode;
import io.github.architrace.otlp.GraphSnapshot;
import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.TimeUnit;

public class ControlPlaneClient implements TransportClient {

  private final ManagedChannel channel;
  private final ControlPlaneServiceGrpc.ControlPlaneServiceStub stub;

  private StreamObserver<AgentRegisterRequestedEvent> outboundObserver;

  @Inject
  public ControlPlaneClient(ManagedChannel channel) {
    this.channel = channel;
    this.stub = ControlPlaneServiceGrpc.newStub(channel);
  }

  @Override
  public StreamObserver<AgentRegisterRequestedEvent> open(StreamObserver<ControlPlaneCommand> inboundObserver) {
    this.outboundObserver = stub.connect(inboundObserver);

    return outboundObserver;
  }

  @Override
  public void close() {
    channel.shutdownNow();
    try {
      channel.awaitTermination(2, TimeUnit.SECONDS);
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void send(GraphSnapshot snapshot) {
    if (outboundObserver == null) {
      return;
    }

    AgentRegisterRequestedEvent event = AgentRegisterRequestedEvent.newBuilder()
//        .setGraphSnapshot(toProto(snapshot))
        .build();

    outboundObserver.onNext(event);
  }

//  private GraphSnapshotEvent toProto(GraphSnapshot snapshot) {
//
//    GraphSnapshotEvent.Builder builder = GraphSnapshotEvent.newBuilder();
//
//    for (GraphNode node : snapshot.nodes()) {
//      builder.addNodes(
//          GraphNodeEvent.newBuilder()
//              .setId(node.id())
//              .setType(node.type().name())
//              .setName(node.name())
//              .build()
//      );
//    }
//
//    snapshot.edges().forEach((key, metrics) -> {
//
//      builder.addEdges(
//          GraphEdgeEvent.newBuilder()
//              .setFrom(key.from())
//              .setTo(key.to())
//              .setRps(metrics.getRps())
//              .setErrorRate(metrics.getErrorRate())
//              .build()
//      );
//
//    });
//
//    return builder.build();
//  }
}
