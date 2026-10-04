/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.span;

import java.util.Objects;
import java.util.Optional;

public sealed interface Peer {

    record Http(String address, Optional<Integer> port) implements Peer {
        public Http {
            Objects.requireNonNull(address, "address");
            Objects.requireNonNull(port, "port");
        }
    }

    record Database(String system, Optional<String> namespace) implements Peer {
        public Database {
            Objects.requireNonNull(system, "system");
            Objects.requireNonNull(namespace, "namespace");
        }
    }

    record Messaging(String system, Optional<String> destination, Optional<String> operation) implements Peer {
        public Messaging {
            Objects.requireNonNull(system, "system");
            Objects.requireNonNull(destination, "destination");
            Objects.requireNonNull(operation, "operation");
        }
    }

    record None() implements Peer {}
}
