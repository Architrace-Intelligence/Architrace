/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.cli;

import com.google.inject.Inject;
import io.github.architrace.runtime.AgentRuntimeService;
import java.io.File;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "run",
    description = "Start Architrace runtime agent"
)
public class RunCommand implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(RunCommand.class);
  public static final String ARCHITRACE_RUNTIME_SHUTDOWN =
      "architrace-runtime-shutdown";

  private final AgentRuntimeService runtimeService;

  @Option(
      names = "--config",
      required = true,
      description = "Path to YAML config file"
  )
  private File configFile;

  @Inject
  public RunCommand(AgentRuntimeService runtimeService) {
    this.runtimeService = runtimeService;
  }

  @Override
  public void run() {
    Path configPath = configFile.toPath();
    log.info("Starting Architrace runtime with config={}", configPath);

    try {
      runtimeService.run(configPath);
      log.info("Architrace runtime terminated normally.");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Architrace runtime interrupted.", e);
    } catch (Exception e) {
      throw new IllegalStateException("Architrace runtime failed.", e);
    }
  }
}
