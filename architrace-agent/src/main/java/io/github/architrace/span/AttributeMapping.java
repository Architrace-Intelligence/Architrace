/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.span;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record AttributeMapping(Map<MappedField, List<String>> keysByField) {

    public AttributeMapping {
        if (keysByField.size() != MappedField.values().length) {
            throw new IllegalArgumentException("Every mapped field needs a key list");
        }
        keysByField = Collections.unmodifiableMap(new EnumMap<>(keysByField));
    }

    public static AttributeMapping defaults() {
        return new AttributeMapping(Arrays.stream(MappedField.values())
                .collect(Collectors.toMap(
                        Function.identity(),
                        MappedField::defaultKeys,
                        (first, _) -> first,
                        () -> new EnumMap<>(MappedField.class))));
    }

    public AttributeMapping with(Map<MappedField, List<String>> overrides) {
        Map<MappedField, List<String>> merged = new EnumMap<>(keysByField);
        overrides.forEach((field, keys) -> merged.put(field, List.copyOf(keys)));
        return new AttributeMapping(merged);
    }

    public List<String> keys(MappedField field) {
        return keysByField.get(field);
    }

    public Map<String, List<String>> byConfigKey() {
        return keysByField.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().configKey(),
                        Map.Entry::getValue,
                        (first, _) -> first,
                        LinkedHashMap::new));
    }
}
