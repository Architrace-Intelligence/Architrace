/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace.cli;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "dry-run",
    description = "Start Architrace runtime agent"
)
public class DryRunCommand implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(DryRunCommand.class);

  @Option(
      names = "--config",
      required = true,
      description = "Path to YAML config file"
  )
  private String configPath;

  @Option(
      names = "--prop",
      description = "Override property (key=value)"
  )
  private Map<String, String> properties;

  @Override
  public void run() {
    if (properties != null && !properties.isEmpty()) {
      log.warn("CLI overrides are not applied yet. Received {} override(s).", properties.size());
    }

    log.info("Validating config has started.");
//    log.info(
//        "Config validation passed. environment='{}', clusterId='{}', domainId='{}', namespace='{}', agent.name='{}', control.plane-bootstrap.server='{}'",
//        config.environment(),
//        config.clusterId(),
//        config.domainId(),
//        config.namespace(),
//        config.agentName(),
//        config.controlPlaneServer());
  }
}
