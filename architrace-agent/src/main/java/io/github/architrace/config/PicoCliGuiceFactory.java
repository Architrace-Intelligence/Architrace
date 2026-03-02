/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.config;

import com.google.inject.Injector;
import java.util.Objects;
import picocli.CommandLine.IFactory;

public class PicoCliGuiceFactory implements IFactory {

  private final Injector injector;

  public PicoCliGuiceFactory(Injector injector) {
    this.injector = Objects.requireNonNull(injector, "injector");
  }

  @Override
  public <K> K create(Class<K> cls) {
    return injector.getInstance(cls);
  }
}

