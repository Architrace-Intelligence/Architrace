/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.cli;

import io.github.architrace.core.BuildVersion;
import picocli.CommandLine.IVersionProvider;

public class BuildVersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() {
        return new String[] {BuildVersion.describe()};
    }
}
