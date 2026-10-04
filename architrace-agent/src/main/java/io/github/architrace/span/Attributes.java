/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.span;

import io.opentelemetry.proto.common.v1.AnyValue;
import io.opentelemetry.proto.common.v1.KeyValue;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public record Attributes(Map<String, String> values) {

    public Attributes {
        values = Map.copyOf(values);
    }

    public static Attributes of(List<KeyValue> attributes) {
        return new Attributes(attributes.stream()
                .flatMap(attribute ->
                        text(attribute.getValue()).map(value -> Map.entry(attribute.getKey(), value)).stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, _) -> first)));
    }

    public Optional<String> first(List<String> keys) {
        return keys.stream()
                .map(values::get)
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private static Optional<String> text(AnyValue value) {
        return switch (value.getValueCase()) {
            case STRING_VALUE -> Optional.of(value.getStringValue());
            case INT_VALUE -> Optional.of(Long.toString(value.getIntValue()));
            case BOOL_VALUE -> Optional.of(Boolean.toString(value.getBoolValue()));
            case DOUBLE_VALUE -> Optional.of(Double.toString(value.getDoubleValue()));
            case ARRAY_VALUE, KVLIST_VALUE, BYTES_VALUE, STRING_VALUE_STRINDEX, VALUE_NOT_SET -> Optional.empty();
        };
    }
}
