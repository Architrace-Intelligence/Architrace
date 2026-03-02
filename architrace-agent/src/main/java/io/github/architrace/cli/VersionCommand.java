/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;

@Command(
    name = "version",
    description = "Print version information"
)
public class VersionCommand implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(VersionCommand.class);

  @Override
  public void run() {
    log.info("Architrace version 0.1.0");
  }
}
