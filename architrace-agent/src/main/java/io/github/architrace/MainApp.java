/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmitry Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.architrace;

import com.google.inject.Guice;
import io.github.architrace.cli.DryRunCommand;
import io.github.architrace.cli.RunCommand;
import io.github.architrace.cli.VersionCommand;
import io.github.architrace.config.AgentModule;
import io.github.architrace.config.PicoCliGuiceFactory;
import java.util.Objects;
import java.util.function.IntConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "architrace",
    mixinStandardHelpOptions = true,
    version = "Architrace 0.1.0",
    description = "Architecture Intelligence CLI",
    subcommands = {
        VersionCommand.class,
        RunCommand.class,
        DryRunCommand.class
    }
)
public class MainApp implements Runnable {

  private static final Logger log = LoggerFactory.getLogger(MainApp.class);

  private static IntConsumer exitHandler = System::exit;

  static void main(String[] args) {
    int exitCode = execute(args);
    exitHandler.accept(exitCode);
  }

  static int execute(String[] args) {
    var injector = Guice.createInjector(new AgentModule());
    CommandLine cmd =
        new CommandLine(
            injector.getInstance(MainApp.class),
            new PicoCliGuiceFactory(injector));

    cmd.setExecutionExceptionHandler((ex, commandLine, parseResult) -> {
      log.error("Execution failed: {}", ex.getMessage(), ex);
      return CommandLine.ExitCode.SOFTWARE;
    });

    cmd.setParameterExceptionHandler((ex, args1) -> {
      log.error("Parameter parsing failed: {}", ex.getMessage(), ex);
      ex.getCommandLine().usage(ex.getCommandLine().getErr());
      return CommandLine.ExitCode.SOFTWARE;
    });

    return cmd.execute(args);
  }

  static void setExitHandler(IntConsumer newHandler) {
    exitHandler = Objects.requireNonNull(newHandler);
  }

  static void resetExitHandler() {
    exitHandler = System::exit;
  }

  @Override
  public void run() {
    log.info("No command specified. Use --help for usage information.");
  }
}
