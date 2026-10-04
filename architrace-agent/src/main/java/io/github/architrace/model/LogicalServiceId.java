/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.model;

import io.github.architrace.span.ServiceIdentity;

public record LogicalServiceId(String environment, String domainId, String serviceName) {

    public static LogicalServiceId of(ServiceIdentity service) {
        return new LogicalServiceId(service.environment(), service.domain(), service.name());
    }

    public String asString() {
        return environment + ":" + domainId + ":" + serviceName;
    }

    @Override
    public String toString() {
        return asString();
    }
}
