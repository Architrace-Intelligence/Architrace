/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.core;

public final class BuildVersion {

  public static final String DEVELOPMENT = "dev";
  private static final String PRODUCT = "Architrace";

  private BuildVersion() {}

  public static String current() {
    String version = BuildVersion.class.getPackage().getImplementationVersion();
    return version == null ? DEVELOPMENT : version;
  }

  public static String describe() {
    return PRODUCT + " " + current();
  }
}
