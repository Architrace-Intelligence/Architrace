/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.graph;

import java.util.List;
import java.util.Locale;

public final class TopicFilter {

    private static final TopicFilter NONE = new TopicFilter(List.of());
    private static final char WILDCARD = '*';

    private final List<String> globs;

    public TopicFilter(List<String> globs) {
        this.globs = globs.stream().map(TopicFilter::fold).toList();
    }

    public static TopicFilter none() {
        return NONE;
    }

    public boolean ignores(String topic) {
        String folded = fold(topic);
        return globs.stream().anyMatch(glob -> matches(glob, folded));
    }

    private static String fold(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static boolean matches(String glob, String topic) {
        int g = 0;
        int t = 0;
        int star = -1;
        int mark = 0;
        while (t < topic.length()) {
            if (g < glob.length() && glob.charAt(g) == WILDCARD) {
                star = g++;
                mark = t;
            } else if (g < glob.length() && glob.charAt(g) == topic.charAt(t)) {
                g++;
                t++;
            } else if (star >= 0) {
                g = star + 1;
                t = ++mark;
            } else {
                return false;
            }
        }
        while (g < glob.length() && glob.charAt(g) == WILDCARD) {
            g++;
        }
        return g == glob.length();
    }
}
