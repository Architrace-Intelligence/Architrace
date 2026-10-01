/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.control.plane.topology.retention;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "architrace.topology.retention")
public record RetentionProperties(
    @DefaultValue("30d") Duration period, @DefaultValue("1000") int batchSize) {

  public RetentionProperties {
    Objects.requireNonNull(period, "period");
    if (period.isZero() || period.isNegative()) {
      throw new IllegalArgumentException("period must be positive");
    }
    if (batchSize <= 0) {
      throw new IllegalArgumentException("batchSize must be positive");
    }
  }
}
