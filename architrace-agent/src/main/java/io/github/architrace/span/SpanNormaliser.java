/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.span;

import com.google.protobuf.ByteString;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Status;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class SpanNormaliser {

    static final String DEFAULT_DOMAIN = "default";
    static final String UNKNOWN_VERSION = "unknown";

    private final AttributeMapping mapping;
    private final String defaultEnvironment;
    private final String defaultCluster;

    public SpanNormaliser(AttributeMapping mapping, String defaultEnvironment, String defaultCluster) {
        this.mapping = Objects.requireNonNull(mapping, "mapping");
        this.defaultEnvironment = Objects.requireNonNull(defaultEnvironment, "defaultEnvironment");
        this.defaultCluster = Objects.requireNonNull(defaultCluster, "defaultCluster");
    }

    public List<SpanRecord> normalise(ExportTraceServiceRequest request) {
        return request.getResourceSpansList().stream().flatMap(this::normalise).toList();
    }

    private Stream<SpanRecord> normalise(ResourceSpans resourceSpans) {
        Attributes resource = Attributes.of(resourceSpans.getResource().getAttributesList());
        Deployment deployment = deployment(resource);
        return service(resource).stream()
                .flatMap(service -> resourceSpans.getScopeSpansList().stream()
                        .flatMap(scopeSpans -> scopeSpans.getSpansList().stream())
                        .map(span -> toRecord(span, service, deployment)));
    }

    private Optional<ServiceIdentity> service(Attributes resource) {
        return resolve(resource, MappedField.SERVICE)
                .map(name -> new ServiceIdentity(
                        resolve(resource, MappedField.ENVIRONMENT).orElse(defaultEnvironment),
                        resolve(resource, MappedField.DOMAIN).orElse(DEFAULT_DOMAIN),
                        name,
                        resolve(resource, MappedField.VERSION).orElse(UNKNOWN_VERSION)));
    }

    private Deployment deployment(Attributes resource) {
        return new Deployment(
                resolve(resource, MappedField.CLUSTER).orElse(defaultCluster),
                resolve(resource, MappedField.NAMESPACE),
                resolve(resource, MappedField.INSTANCE));
    }

    private SpanRecord toRecord(Span span, ServiceIdentity service, Deployment deployment) {
        Attributes attributes = Attributes.of(span.getAttributesList());
        return new SpanRecord(
                hex(span.getTraceId()),
                hex(span.getSpanId()),
                Optional.of(span.getParentSpanId()).filter(id -> !id.isEmpty()).map(SpanNormaliser::hex),
                kind(span.getKind()),
                span.getStartTimeUnixNano(),
                span.getEndTimeUnixNano(),
                span.getStatus().getCode() == Status.StatusCode.STATUS_CODE_ERROR,
                service,
                deployment,
                peer(attributes));
    }

    private Peer peer(Attributes attributes) {
        return database(attributes)
                .or(() -> messaging(attributes))
                .or(() -> http(attributes))
                .orElseGet(Peer.None::new);
    }

    private Optional<Peer> database(Attributes attributes) {
        return resolve(attributes, MappedField.DB_SYSTEM)
                .map(system -> new Peer.Database(system, resolve(attributes, MappedField.DB_NAMESPACE)));
    }

    private Optional<Peer> messaging(Attributes attributes) {
        return resolve(attributes, MappedField.MESSAGING_SYSTEM)
                .map(system -> new Peer.Messaging(
                        system,
                        resolve(attributes, MappedField.MESSAGING_DESTINATION),
                        resolve(attributes, MappedField.MESSAGING_OPERATION)));
    }

    private Optional<Peer> http(Attributes attributes) {
        return resolve(attributes, MappedField.HTTP_ADDRESS)
                .map(address -> new Peer.Http(
                        address, resolve(attributes, MappedField.HTTP_PORT).flatMap(SpanNormaliser::port)));
    }

    private Optional<String> resolve(Attributes attributes, MappedField field) {
        return attributes.first(mapping.keys(field));
    }

    private static Optional<Integer> port(String value) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException _) {
            return Optional.empty();
        }
    }

    private static SpanKind kind(Span.SpanKind kind) {
        return switch (kind) {
            case SPAN_KIND_CLIENT -> SpanKind.CLIENT;
            case SPAN_KIND_SERVER -> SpanKind.SERVER;
            case SPAN_KIND_PRODUCER -> SpanKind.PRODUCER;
            case SPAN_KIND_CONSUMER -> SpanKind.CONSUMER;
            case SPAN_KIND_INTERNAL, SPAN_KIND_UNSPECIFIED, UNRECOGNIZED -> SpanKind.INTERNAL;
        };
    }

    private static String hex(ByteString bytes) {
        return HexFormat.of().formatHex(bytes.toByteArray());
    }
}
