/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.cli;

import com.google.inject.Inject;
import io.github.architrace.core.config.AgentConfig;
import io.github.architrace.core.config.AgentConfigException;
import io.github.architrace.core.config.AgentConfigLoader;
import io.github.architrace.service.runtime.AgentRuntimeService;
import java.util.Objects;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(name = "run", description = "Start the Architrace agent")
public final class RunCommand implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(RunCommand.class);

    @Mixin
    private ConfigOptions configOptions;

    @Spec
    private CommandSpec spec;

    private final AgentConfigLoader configLoader;
    private final AgentRuntimeService runtimeService;

    @Inject
    public RunCommand(AgentConfigLoader configLoader, AgentRuntimeService runtimeService) {
        this.configLoader = Objects.requireNonNull(configLoader, "configLoader");
        this.runtimeService = Objects.requireNonNull(runtimeService, "runtimeService");
    }

    @Override
    public Integer call() throws InterruptedException {
        AgentConfig config;
        try {
            config = configOptions.load(configLoader);
        } catch (AgentConfigException e) {
            return ConfigOptions.reportInvalid(e, spec.commandLine().getErr());
        }
        log.info(
                "Starting the Architrace agent '{}' for scope {}/{}/{}",
                config.agentName(),
                config.project(),
                config.environment(),
                config.cluster());
        runtimeService.run(config);
        log.info("Architrace agent terminated normally.");
        return CommandLine.ExitCode.OK;
    }
}
