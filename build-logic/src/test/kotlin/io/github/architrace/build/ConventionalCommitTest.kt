/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.build

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConventionalCommitTest {

    @Test
    fun `parses type, scope and subject`() {
        val commit = ConventionalCommit.parse("feat(ARCHI-32): Scaffold the web UI module (#39)\n\nBody text.")

        assertThat(commit.type).isEqualTo("feat")
        assertThat(commit.scope).isEqualTo("ARCHI-32")
        assertThat(commit.subject).isEqualTo("Scaffold the web UI module (#39)")
        assertThat(commit.breaking).isFalse()
        assertThat(commit.bump).isEqualTo(Bump.MINOR)
    }

    @Test
    fun `maps every releasing type to its bump`() {
        assertThat(ConventionalCommit.parse("fix: a").bump).isEqualTo(Bump.PATCH)
        assertThat(ConventionalCommit.parse("perf: a").bump).isEqualTo(Bump.PATCH)
        assertThat(ConventionalCommit.parse("refactor: a").bump).isEqualTo(Bump.PATCH)
        assertThat(ConventionalCommit.parse("build(deps): a").bump).isEqualTo(Bump.PATCH)
        assertThat(ConventionalCommit.parse("ci: a").bump).isEqualTo(Bump.NONE)
        assertThat(ConventionalCommit.parse("docs: a").bump).isEqualTo(Bump.NONE)
        assertThat(ConventionalCommit.parse("test: a").bump).isEqualTo(Bump.NONE)
        assertThat(ConventionalCommit.parse("chore: a").bump).isEqualTo(Bump.NONE)
    }

    @Test
    fun `treats a bang or a footer as a breaking change`() {
        assertThat(ConventionalCommit.parse("refactor(ARCHI-9)!: Drop the v0 contract").bump).isEqualTo(Bump.MAJOR)
        assertThat(ConventionalCommit.parse("fix: Rename a field\n\nBREAKING CHANGE: clients must rename too").bump)
            .isEqualTo(Bump.MAJOR)
        assertThat(ConventionalCommit.parse("fix: Rename a field\n\nBREAKING-CHANGE: clients must rename too").bump)
            .isEqualTo(Bump.MAJOR)
    }

    @Test
    fun `keeps non-conventional messages as other commits without a bump`() {
        val merge = ConventionalCommit.parse("Merge pull request #38 from Architrace-Intelligence/ARCHI-31")

        assertThat(merge.conventional).isFalse()
        assertThat(merge.subject).startsWith("Merge pull request #38")
        assertThat(merge.bump).isEqualTo(Bump.NONE)
        assertThat(ConventionalCommit.parse("Feat: capitalised type is not conventional").conventional).isFalse()
    }

    @Test
    fun `takes the highest bump across commits`() {
        val commits = listOf("docs: a", "fix: b", "feat: c", "chore: d").map(ConventionalCommit::parse)

        assertThat(ConventionalCommit.highestBump(commits)).isEqualTo(Bump.MINOR)
        assertThat(ConventionalCommit.highestBump(emptyList())).isEqualTo(Bump.NONE)
    }
}
