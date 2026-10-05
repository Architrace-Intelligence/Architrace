/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.topology;

import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "architrace.topology")
public record TopologyProperties(@DefaultValue List<String> platformHosts) {

    public TopologyProperties {
        Objects.requireNonNull(platformHosts, "platformHosts");
        platformHosts = List.copyOf(platformHosts);
    }
}
