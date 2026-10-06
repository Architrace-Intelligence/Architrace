/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class TopicFilter {

    private static final TopicFilter NONE = new TopicFilter(List.of());
    private static final String WILDCARD = "*";

    private final List<Pattern> patterns;

    public TopicFilter(List<String> globs) {
        this.patterns = globs.stream().map(TopicFilter::compile).toList();
    }

    public static TopicFilter none() {
        return NONE;
    }

    public boolean ignores(String topic) {
        return patterns.stream().anyMatch(pattern -> pattern.matcher(topic).matches());
    }

    private static Pattern compile(String glob) {
        return Pattern.compile(
                Arrays.stream(glob.split(Pattern.quote(WILDCARD), -1))
                        .map(Pattern::quote)
                        .collect(Collectors.joining(".*")),
                Pattern.CASE_INSENSITIVE);
    }
}
