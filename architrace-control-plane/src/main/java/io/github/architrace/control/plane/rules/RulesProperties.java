/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.control.plane.rules;

import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "architrace.rules")
public record RulesProperties(
        @DefaultValue SharedDatabaseProperties sharedDatabase,
        @DefaultValue UnknownExternalProperties unknownExternal,
        @DefaultValue WideBlastRadiusProperties wideBlastRadius) {

    public RulesProperties {
        Objects.requireNonNull(sharedDatabase, "sharedDatabase");
        Objects.requireNonNull(unknownExternal, "unknownExternal");
        Objects.requireNonNull(wideBlastRadius, "wideBlastRadius");
    }

    public static RulesProperties defaults() {
        return new RulesProperties(
                new SharedDatabaseProperties(2),
                new UnknownExternalProperties(List.of()),
                new WideBlastRadiusProperties(50, 3));
    }

    public record SharedDatabaseProperties(
            @DefaultValue("2") int minServices) {

        public SharedDatabaseProperties {
            if (minServices < 2) {
                throw new IllegalArgumentException("minServices must be at least 2");
            }
        }
    }

    public record UnknownExternalProperties(@DefaultValue List<String> allowlist) {

        public UnknownExternalProperties {
            allowlist = List.copyOf(allowlist);
        }
    }

    public record WideBlastRadiusProperties(
            @DefaultValue("50") int minSharePercent,
            @DefaultValue("3") int minServices) {

        public WideBlastRadiusProperties {
            if (minSharePercent < 1 || minSharePercent > 100) {
                throw new IllegalArgumentException("minSharePercent must be between 1 and 100");
            }
            if (minServices < 1) {
                throw new IllegalArgumentException("minServices must be at least 1");
            }
        }
    }
}
