/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.span;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum MappedField {
    ENVIRONMENT("environment", "deployment.environment.name", "deployment.environment", "environment"),
    DOMAIN("domain", "service.namespace", "domainId"),
    SERVICE("service", "service.name", "serviceName"),
    VERSION("version", "service.version"),
    CLUSTER("cluster", "k8s.cluster.name", "cluster"),
    NAMESPACE("namespace", "k8s.namespace.name", "namespace"),
    INSTANCE("instance", "service.instance.id", "k8s.pod.name"),
    HTTP_ADDRESS("http-address", "server.address", "net.peer.name", "http.host"),
    HTTP_PORT("http-port", "server.port", "net.peer.port"),
    DB_SYSTEM("db-system", "db.system"),
    DB_NAMESPACE("db-namespace", "db.namespace", "db.name"),
    MESSAGING_SYSTEM("messaging-system", "messaging.system"),
    MESSAGING_DESTINATION("messaging-destination", "messaging.destination.name", "messaging.destination"),
    MESSAGING_OPERATION("messaging-operation", "messaging.operation.type", "messaging.operation");

    private final String configKey;
    private final List<String> defaultKeys;

    MappedField(String configKey, String... defaultKeys) {
        this.configKey = configKey;
        this.defaultKeys = List.of(defaultKeys);
    }

    public String configKey() {
        return configKey;
    }

    public List<String> defaultKeys() {
        return defaultKeys;
    }

    public static Optional<MappedField> byConfigKey(String configKey) {
        return Arrays.stream(values())
                .filter(field -> field.configKey.equals(configKey))
                .findFirst();
    }

    public static String configKeys() {
        return Arrays.stream(values()).map(MappedField::configKey).collect(Collectors.joining(", "));
    }
}
