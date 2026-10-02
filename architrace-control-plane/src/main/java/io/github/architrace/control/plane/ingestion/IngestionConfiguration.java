/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.ingestion;

import io.github.architrace.control.plane.topology.AgentLiveness;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(IngestionProperties.class)
public class IngestionConfiguration {

    @Bean
    AgentLiveness agentLiveness(IngestionProperties properties) {
        return new AgentLiveness(properties.heartbeatInterval());
    }
}
