/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.span;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AttributeMappingTest {

    @Test
    void defaultsCoverEveryFieldWithTheDocumentedKeys() {
        AttributeMapping mapping = AttributeMapping.defaults();

        assertThat(mapping.keysByField()).hasSize(MappedField.values().length);
        assertThat(mapping.keys(MappedField.ENVIRONMENT))
                .containsExactly("deployment.environment.name", "deployment.environment", "environment");
        assertThat(mapping.keys(MappedField.SERVICE)).containsExactly("service.name", "serviceName");
        assertThat(mapping.keys(MappedField.HTTP_ADDRESS))
                .containsExactly("server.address", "net.peer.name", "http.host");
        assertThat(mapping.keys(MappedField.MESSAGING_DESTINATION))
                .containsExactly("messaging.destination.name", "messaging.destination");
    }

    @Test
    void overridesReplaceTheKeysOfOneFieldOnly() {
        AttributeMapping mapping = AttributeMapping.defaults().with(Map.of(MappedField.DOMAIN, List.of("team")));

        assertThat(mapping.keys(MappedField.DOMAIN)).containsExactly("team");
        assertThat(mapping.keys(MappedField.SERVICE)).isEqualTo(MappedField.SERVICE.defaultKeys());
    }

    @Test
    void configViewListsFieldsByConfigKeyInDeclarationOrder() {
        Map<String, List<String>> view = AttributeMapping.defaults().byConfigKey();

        assertThat(view.keySet())
                .containsExactly(
                        "environment",
                        "domain",
                        "service",
                        "version",
                        "cluster",
                        "namespace",
                        "instance",
                        "http-address",
                        "http-port",
                        "db-system",
                        "db-namespace",
                        "messaging-system",
                        "messaging-destination",
                        "messaging-operation");
        assertThat(view.get("db-namespace")).containsExactly("db.namespace", "db.name");
    }

    @Test
    void mappingRejectsAnIncompleteKeyTable() {
        Map<MappedField, List<String>> incomplete = Map.of(MappedField.SERVICE, List.of("service.name"));

        assertThatIllegalArgumentException().isThrownBy(() -> new AttributeMapping(incomplete));
    }

    @Test
    void fieldsAreLookedUpByConfigKey() {
        assertThat(MappedField.byConfigKey("messaging-system")).contains(MappedField.MESSAGING_SYSTEM);
        assertThat(MappedField.byConfigKey("unknown")).isEmpty();
        assertThat(MappedField.configKeys()).startsWith("environment, domain, service");
    }
}
