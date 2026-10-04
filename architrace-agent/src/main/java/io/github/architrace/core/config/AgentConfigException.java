/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.core.config;

import java.util.List;

public final class AgentConfigException extends RuntimeException {

    private final transient List<String> problems;

    public AgentConfigException(List<String> problems) {
        super(String.join("; ", problems));
        this.problems = List.copyOf(problems);
    }

    public AgentConfigException(String problem) {
        this(List.of(problem));
    }

    public List<String> problems() {
        return problems;
    }
}
