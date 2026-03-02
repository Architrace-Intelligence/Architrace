/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.config;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import io.github.architrace.inbound.ControlMessageHandler;

public class AgentModule extends AbstractModule {

  @Override
  protected void configure() {
    Multibinder.newSetBinder(binder(), ControlMessageHandler.class);
  }
}
