/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.build

import com.github.zafarkhaja.semver.Version
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jgit.api.Git
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import pl.allegro.tech.build.axion.release.domain.VersionIncrementerContext
import pl.allegro.tech.build.axion.release.domain.scm.ScmPosition

class GitHistoryTest {

    @TempDir
    lateinit var directory: File

    @Test
    fun `lists the commits after a tag newest first and bumps from them`() {
        Git.init().setDirectory(directory).call().use { git ->
            git.commit("chore: Initial import")
            git.commit("feat(ARCHI-1): First feature")
            git.tag().setName("v0.1.0").call()
            git.commit("fix(ARCHI-2): A fix")
            git.commit("docs(ARCHI-3): A page")
            git.remoteAdd().setName("origin").setUri(org.eclipse.jgit.transport.URIish("git@github.com:Org/Repo.git")).call()
        }
        val history = GitHistory(directory)

        assertThat(history.hasTag("v0.1.0")).isTrue()
        assertThat(history.hasTag("v0.2.0")).isFalse()
        assertThat(history.commitsSince("v0.1.0")).containsExactly("docs(ARCHI-3): A page", "fix(ARCHI-2): A fix")
        assertThat(history.commitsSince("v9.9.9")).hasSize(4)
        assertThat(history.remoteUrl()).isEqualTo("https://github.com/Org/Repo")

        val incrementer = ConventionalCommitsIncrementer(history, "v")
        assertThat(incrementer.apply(context("0.1.0"))).isEqualTo(Version.valueOf("0.1.1"))
        assertThat(incrementer.apply(context("0.2.0"))).isEqualTo(Version.valueOf("0.2.0"))
    }

    @Test
    fun `answers nothing outside a repository`() {
        val history = GitHistory(File(directory, "elsewhere"))

        assertThat(history.hasTag("v0.1.0")).isFalse()
        assertThat(history.commitsSince("v0.1.0")).isEmpty()
        assertThat(history.remoteUrl()).isNull()
    }

    @Test
    fun `turns remote urls into web urls`() {
        assertThat(GitHistory.toWebUrl("https://github.com/Org/Repo.git")).isEqualTo("https://github.com/Org/Repo")
        assertThat(GitHistory.toWebUrl("ssh://git@github.com/Org/Repo")).isEqualTo("https://github.com/Org/Repo")
        assertThat(GitHistory.toWebUrl("file:///tmp/repo")).isNull()
    }

    private fun Git.commit(message: String) {
        commit().setAllowEmpty(true).setAuthor("Test", "test@example.com").setCommitter("Test", "test@example.com").setMessage(message).call()
    }

    private fun context(version: String) = VersionIncrementerContext(Version.valueOf(version), ScmPosition("abc1234def", "main"))
}
