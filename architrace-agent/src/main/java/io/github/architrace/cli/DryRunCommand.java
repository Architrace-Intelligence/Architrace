/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.cli;

import com.google.inject.Inject;
import io.github.architrace.core.config.AgentConfig;
import io.github.architrace.core.config.AgentConfigException;
import io.github.architrace.core.config.AgentConfigLoader;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(name = "dry-run", description = "Load and validate the configuration, then print the effective values")
public final class DryRunCommand implements Callable<Integer> {

    @Mixin
    private ConfigOptions configOptions;

    @Spec
    private CommandSpec spec;

    private final AgentConfigLoader configLoader;

    @Inject
    public DryRunCommand(AgentConfigLoader configLoader) {
        this.configLoader = Objects.requireNonNull(configLoader, "configLoader");
    }

    @Override
    public Integer call() {
        try {
            AgentConfig config = configOptions.load(configLoader);
            PrintWriter out = spec.commandLine().getOut();
            out.print(configLoader.render(config));
            out.flush();
            return CommandLine.ExitCode.OK;
        } catch (AgentConfigException e) {
            return ConfigOptions.reportInvalid(e, spec.commandLine().getErr());
        }
    }
}
