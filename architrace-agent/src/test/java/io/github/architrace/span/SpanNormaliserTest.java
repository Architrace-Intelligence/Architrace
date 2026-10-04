/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.span;

import static io.github.architrace.testsupport.OtlpRequests.CLIENT_SPAN_ID;
import static io.github.architrace.testsupport.OtlpRequests.SERVER_SPAN_ID;
import static io.github.architrace.testsupport.OtlpRequests.TRACE_ID;
import static io.github.architrace.testsupport.OtlpRequests.childSpan;
import static io.github.architrace.testsupport.OtlpRequests.failed;
import static io.github.architrace.testsupport.OtlpRequests.flag;
import static io.github.architrace.testsupport.OtlpRequests.fraction;
import static io.github.architrace.testsupport.OtlpRequests.list;
import static io.github.architrace.testsupport.OtlpRequests.number;
import static io.github.architrace.testsupport.OtlpRequests.request;
import static io.github.architrace.testsupport.OtlpRequests.span;
import static io.github.architrace.testsupport.OtlpRequests.standardResource;
import static io.github.architrace.testsupport.OtlpRequests.text;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.Span;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SpanNormaliserTest {

    private final SpanNormaliser sut = new SpanNormaliser(AttributeMapping.defaults(), "DEV", "local");

    @Test
    void normalisesIdentityDeploymentTimingAndStatusFromStandardConventions() {
        Span.Builder server = failed(
                        childSpan(Span.SpanKind.SPAN_KIND_SERVER, TRACE_ID, SERVER_SPAN_ID, CLIENT_SPAN_ID))
                .setStartTimeUnixNano(5_000_000_000L)
                .setEndTimeUnixNano(5_120_000_000L);

        List<SpanRecord> records = sut.normalise(request(standardResource("checkout"), server));

        assertThat(records).singleElement().satisfies(normalised -> {
            assertThat(normalised.traceId()).isEqualTo(TRACE_ID);
            assertThat(normalised.spanId()).isEqualTo(SERVER_SPAN_ID);
            assertThat(normalised.parentSpanId()).contains(CLIENT_SPAN_ID);
            assertThat(normalised.kind()).isEqualTo(SpanKind.SERVER);
            assertThat(normalised.startEpochNanos()).isEqualTo(5_000_000_000L);
            assertThat(normalised.endEpochNanos()).isEqualTo(5_120_000_000L);
            assertThat(normalised.latencyMillis()).isEqualTo(120L);
            assertThat(normalised.error()).isTrue();
            assertThat(normalised.service()).isEqualTo(new ServiceIdentity("PROD", "shop", "checkout", "1.4.2"));
            assertThat(normalised.deployment())
                    .isEqualTo(new Deployment("eu-1", Optional.of("checkout"), Optional.of("checkout-7d9f")));
            assertThat(normalised.peer()).isEqualTo(new Peer.None());
        });
    }

    @Test
    void fallsBackToLegacyKeysAndConfiguredDefaults() {
        List<SpanRecord> records = sut.normalise(request(
                List.of(text("serviceName", "orders"), text("domainId", "sales"), text("namespace", "team-a")),
                span(Span.SpanKind.SPAN_KIND_INTERNAL, TRACE_ID, SERVER_SPAN_ID)));

        assertThat(records).singleElement().satisfies(normalised -> {
            assertThat(normalised.service()).isEqualTo(new ServiceIdentity("DEV", "sales", "orders", "unknown"));
            assertThat(normalised.deployment())
                    .isEqualTo(new Deployment("local", Optional.of("team-a"), Optional.empty()));
            assertThat(normalised.parentSpanId()).isEmpty();
            assertThat(normalised.kind()).isEqualTo(SpanKind.INTERNAL);
            assertThat(normalised.error()).isFalse();
        });
    }

    @Test
    void usesDefaultDomainWhenNoNamespaceIsReported() {
        List<SpanRecord> records = sut.normalise(request(
                List.of(text("service.name", "orders")),
                span(Span.SpanKind.SPAN_KIND_UNSPECIFIED, TRACE_ID, SERVER_SPAN_ID)));

        assertThat(records).singleElement().satisfies(normalised -> {
            assertThat(normalised.service().domain()).isEqualTo("default");
            assertThat(normalised.kind()).isEqualTo(SpanKind.INTERNAL);
        });
    }

    @Test
    void dropsResourcesWithoutAServiceName() {
        ExportTraceServiceRequest request = request(
                List.of(text("service.namespace", "shop")),
                span(Span.SpanKind.SPAN_KIND_SERVER, TRACE_ID, SERVER_SPAN_ID));

        assertThat(sut.normalise(request)).isEmpty();
    }

    @Test
    void classifiesDatabasePeersUnderCurrentAndLegacyKeys() {
        Span.Builder current = span(Span.SpanKind.SPAN_KIND_CLIENT, TRACE_ID, CLIENT_SPAN_ID)
                .addAttributes(text("db.system", "postgresql"))
                .addAttributes(text("db.namespace", "orders"));
        Span.Builder legacy = span(Span.SpanKind.SPAN_KIND_CLIENT, TRACE_ID, SERVER_SPAN_ID)
                .addAttributes(text("db.system", "mysql"))
                .addAttributes(text("db.name", "legacy"));

        List<SpanRecord> records = sut.normalise(request(standardResource("orders"), current, legacy));

        assertThat(records)
                .extracting(SpanRecord::peer)
                .containsExactly(
                        new Peer.Database("postgresql", Optional.of("orders")),
                        new Peer.Database("mysql", Optional.of("legacy")));
    }

    @Test
    void classifiesMessagingPeersUnderCurrentAndLegacyKeys() {
        Span.Builder current = span(Span.SpanKind.SPAN_KIND_PRODUCER, TRACE_ID, CLIENT_SPAN_ID)
                .addAttributes(text("messaging.system", "kafka"))
                .addAttributes(text("messaging.destination.name", "orders"))
                .addAttributes(text("messaging.operation.type", "publish"));
        Span.Builder legacy = span(Span.SpanKind.SPAN_KIND_CONSUMER, TRACE_ID, SERVER_SPAN_ID)
                .addAttributes(text("messaging.system", "rabbitmq"))
                .addAttributes(text("messaging.destination", "invoices"));

        List<SpanRecord> records = sut.normalise(request(standardResource("orders"), current, legacy));

        assertThat(records)
                .extracting(SpanRecord::kind, SpanRecord::peer)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(
                                SpanKind.PRODUCER,
                                new Peer.Messaging("kafka", Optional.of("orders"), Optional.of("publish"))),
                        org.assertj.core.groups.Tuple.tuple(
                                SpanKind.CONSUMER,
                                new Peer.Messaging("rabbitmq", Optional.of("invoices"), Optional.empty())));
    }

    @Test
    void classifiesHttpPeersAndReadsNumericPorts() {
        Span.Builder current = span(Span.SpanKind.SPAN_KIND_CLIENT, TRACE_ID, CLIENT_SPAN_ID)
                .addAttributes(text("server.address", "api.example.com"))
                .addAttributes(number("server.port", 8443));
        Span.Builder legacy = span(Span.SpanKind.SPAN_KIND_CLIENT, TRACE_ID, SERVER_SPAN_ID)
                .addAttributes(text("net.peer.name", "legacy.example.com"))
                .addAttributes(text("net.peer.port", "not-a-port"));

        List<SpanRecord> records = sut.normalise(request(standardResource("orders"), current, legacy));

        assertThat(records)
                .extracting(SpanRecord::peer)
                .containsExactly(
                        new Peer.Http("api.example.com", Optional.of(8443)),
                        new Peer.Http("legacy.example.com", Optional.empty()));
    }

    @Test
    void databaseWinsOverMessagingAndHttpWhenSeveralPeerKindsArePresent() {
        Span.Builder mixed = span(Span.SpanKind.SPAN_KIND_CLIENT, TRACE_ID, CLIENT_SPAN_ID)
                .addAttributes(text("server.address", "db.example.com"))
                .addAttributes(text("messaging.system", "kafka"))
                .addAttributes(text("db.system", "postgresql"));

        List<SpanRecord> records = sut.normalise(request(standardResource("orders"), mixed));

        assertThat(records)
                .singleElement()
                .extracting(SpanRecord::peer)
                .isEqualTo(new Peer.Database("postgresql", Optional.empty()));
    }

    @Test
    void configuredMappingReplacesTheDefaultKeys() {
        SpanNormaliser custom = new SpanNormaliser(
                AttributeMapping.defaults().with(Map.of(MappedField.DOMAIN, List.of("team"))), "DEV", "local");
        List<SpanRecord> records = custom.normalise(request(
                List.of(text("service.name", "orders"), text("service.namespace", "shop"), text("team", "payments")),
                span(Span.SpanKind.SPAN_KIND_SERVER, TRACE_ID, SERVER_SPAN_ID)));

        assertThat(records)
                .singleElement()
                .extracting(normalised -> normalised.service().domain())
                .isEqualTo("payments");
    }

    @Test
    void attributeValuesOfEveryScalarTypeAreReadAndCollectionsAreIgnored() {
        Attributes attributes = Attributes.of(List.of(
                text("text", "value"),
                number("number", 42),
                flag("flag", true),
                fraction("fraction", 0.5),
                list("list", "a", "b"),
                text("blank", " "),
                text("text", "duplicate")));

        assertThat(attributes.values())
                .containsEntry("text", "value")
                .containsEntry("number", "42")
                .containsEntry("flag", "true")
                .containsEntry("fraction", "0.5")
                .containsEntry("blank", " ")
                .doesNotContainKey("list");
        assertThat(attributes.first(List.of("missing", "blank", "number"))).contains("42");
        assertThat(attributes.first(List.of("missing"))).isEmpty();
    }

    @Test
    void latencyNeverGoesNegative() {
        Span.Builder reversed = span(Span.SpanKind.SPAN_KIND_SERVER, TRACE_ID, SERVER_SPAN_ID)
                .setStartTimeUnixNano(9_000_000_000L)
                .setEndTimeUnixNano(1_000_000_000L);

        List<SpanRecord> records = sut.normalise(request(standardResource("orders"), reversed));

        assertThat(records)
                .singleElement()
                .extracting(SpanRecord::latencyMillis)
                .isEqualTo(0L);
    }
}
