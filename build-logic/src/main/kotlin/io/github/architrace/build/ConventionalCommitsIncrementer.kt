/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.build

import com.github.zafarkhaja.semver.Version
import pl.allegro.tech.build.axion.release.domain.VersionIncrementerContext
import pl.allegro.tech.build.axion.release.domain.properties.VersionProperties

class ConventionalCommitsIncrementer(
    private val history: GitHistory,
    private val tagPrefix: String,
) : VersionProperties.Incrementer {

    override fun apply(context: VersionIncrementerContext): Version {
        val current = context.currentVersion
        val tag = "$tagPrefix$current"
        if (!history.hasTag(tag)) {
            return current
        }
        val commits = history.commitsSince(tag).map(ConventionalCommit::parse)
        return when (ConventionalCommit.highestBump(commits)) {
            Bump.MAJOR -> current.incrementMajorVersion()
            Bump.MINOR -> current.incrementMinorVersion()
            Bump.PATCH -> current.incrementPatchVersion()
            Bump.NONE -> current
        }
    }
}
