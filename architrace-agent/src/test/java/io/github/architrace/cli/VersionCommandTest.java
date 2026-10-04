/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class VersionCommandTest {

    @Test
    void printsTheBuildVersionToTheCommandOutput() {
        StringWriter out = new StringWriter();
        CommandLine command = new CommandLine(new VersionCommand());
        command.setOut(new PrintWriter(out, true));

        int exitCode = command.execute();

        assertThat(exitCode).isEqualTo(CommandLine.ExitCode.OK);
        assertThat(out.toString()).isEqualToIgnoringNewLines("Architrace dev");
    }

    @Test
    void providesTheSameVersionForTheStandardOption() {
        assertThat(new BuildVersionProvider().getVersion()).containsExactly("Architrace dev");
    }
}
