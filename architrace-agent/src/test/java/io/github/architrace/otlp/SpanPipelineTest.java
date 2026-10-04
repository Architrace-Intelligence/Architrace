/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.otlp;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.architrace.model.EdgeKey;
import io.github.architrace.service.graph.AsyncDependencyResolver;
import io.github.architrace.service.graph.GlobalSpanRegistry;
import io.github.architrace.service.graph.NodeRegistry;
import io.github.architrace.service.graph.SyncDependencyResolver;
import io.github.architrace.service.processor.AsyncDependencyProcessor;
import io.github.architrace.service.processor.NodeProcessor;
import io.github.architrace.service.processor.SpanBatchProcessor;
import io.github.architrace.service.processor.SyncDependencyProcessor;
import io.github.architrace.snapshot.GraphSnapshotService;
import io.github.architrace.span.Peer;
import io.github.architrace.span.SpanKind;
import io.github.architrace.span.SpanRecord;
import io.github.architrace.testsupport.TestSpans;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SpanPipelineTest {

    private static final String TRACE = "trace-1";
    private static final Peer.Messaging ORDERS_TOPIC =
            new Peer.Messaging("kafka", Optional.of("orders"), Optional.empty());

    private final GlobalSpanRegistry registry = new GlobalSpanRegistry();
    private final SyncDependencyResolver syncResolver = new SyncDependencyResolver(registry);
    private final AsyncDependencyResolver asyncResolver = new AsyncDependencyResolver();
    private final NodeRegistry nodeRegistry = new NodeRegistry();
    private final SpanPipeline sut = new SpanPipeline(List.of(
            new SyncDependencyProcessor(syncResolver),
            new AsyncDependencyProcessor(asyncResolver),
            new NodeProcessor(nodeRegistry)));
    private final GraphSnapshotService snapshots = new GraphSnapshotService(nodeRegistry, syncResolver, asyncResolver);

    @Test
    void clientAndServerSpansOfOneTraceFormASyncEdgeInEitherArrivalOrder() {
        SpanRecord client = TestSpans.span(SpanKind.CLIENT, TRACE, "c1", "checkout");
        SpanRecord server = TestSpans.child(SpanKind.SERVER, TRACE, "s1", "c1", "orders");
        SpanRecord laterClient = TestSpans.span(SpanKind.CLIENT, "trace-2", "c2", "orders");
        SpanRecord laterServer = TestSpans.child(SpanKind.SERVER, "trace-2", "s2", "c2", "billing");

        sut.process(List.of(client, server, laterServer, laterClient));

        assertThat(snapshots.snapshot().edges().keySet())
                .containsExactlyInAnyOrder(
                        new EdgeKey("DEV:default:checkout", "DEV:default:orders"),
                        new EdgeKey("DEV:default:orders", "DEV:default:billing"));
        assertThat(snapshots.snapshot().nodes())
                .extracting(GraphNode::id)
                .containsExactlyInAnyOrder("DEV:default:checkout", "DEV:default:orders", "DEV:default:billing");
    }

    @Test
    void producerAndConsumerOfOneDestinationFormAnAsyncEdge() {
        SpanRecord producer = TestSpans.withPeer(SpanKind.PRODUCER, TRACE, "p1", "checkout", ORDERS_TOPIC);
        SpanRecord consumer = TestSpans.withPeer(SpanKind.CONSUMER, TRACE, "k1", "fulfilment", ORDERS_TOPIC);

        sut.process(List.of(producer, consumer));

        assertThat(snapshots.snapshot().edges().keySet())
                .containsExactly(new EdgeKey("DEV:default:checkout", "DEV:default:fulfilment"));
    }

    @Test
    void spansAcrossEnvironmentsDuplicatesAndInternalSpansProduceNoEdges() {
        SpanRecord client = TestSpans.span(SpanKind.CLIENT, TRACE, "c1", "checkout");
        SpanRecord foreignServer =
                TestSpans.span(SpanKind.SERVER, TRACE, "s1", Optional.of("c1"), "orders", new Peer.None(), "PROD");
        SpanRecord internal = TestSpans.child(SpanKind.INTERNAL, TRACE, "i1", "c1", "checkout");
        SpanRecord selfCall = TestSpans.child(SpanKind.SERVER, TRACE, "s2", "c1", "checkout");

        sut.process(List.of(client, client, foreignServer, internal, selfCall));

        assertThat(snapshots.snapshot().edges()).isEmpty();
    }

    @Test
    void batchProcessorDrainsTheRingBufferIntoThePipeline() throws InterruptedException {
        SpanRingBuffer ringBuffer = new SpanRingBuffer(4);
        SpanBatchProcessor batchProcessor = new SpanBatchProcessor(ringBuffer, sut);
        batchProcessor.submit(List.of(
                TestSpans.span(SpanKind.CLIENT, TRACE, "c1", "checkout"),
                TestSpans.child(SpanKind.SERVER, TRACE, "s1", "c1", "orders")));
        Thread worker = Thread.ofVirtual().start(batchProcessor::run);

        Thread.sleep(100);
        worker.interrupt();
        worker.join(2_000);

        assertThat(worker.isAlive()).isFalse();
        assertThat(snapshots.snapshot().edges()).hasSize(1);
    }

    @Test
    void ringBufferRejectsSpansWhenFull() {
        SpanRingBuffer ringBuffer = new SpanRingBuffer(2);
        SpanRecord span = TestSpans.span(SpanKind.CLIENT, TRACE, "c1", "checkout");

        assertThat(ringBuffer.publish(span)).isTrue();
        assertThat(ringBuffer.publish(span)).isTrue();
        assertThat(ringBuffer.publish(span)).isFalse();
        assertThat(ringBuffer.poll()).isSameAs(span);
        assertThat(ringBuffer.publish(span)).isTrue();
    }
}
