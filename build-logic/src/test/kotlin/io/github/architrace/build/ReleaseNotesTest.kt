/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.build

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ReleaseNotesTest {

    private val notes = ReleaseNotes("https://github.com/Architrace-Intelligence/Architrace")

    @Test
    fun `groups commits by type with breaking changes first and links pull requests`() {
        val rendered =
            notes.render(
                listOf(
                    "docs(ARCHI-40): Describe the release flow (#45)",
                    "Merge pull request #44 from Architrace-Intelligence/ARCHI-39",
                    "feat(ARCHI-39): Add the Projects list (#44)",
                    "fix: Keep the index page uncached",
                    "refactor(ARCHI-38)!: Drop the v0 contract (#43)",
                ),
            )

        assertThat(rendered)
            .isEqualTo(
                """
                ## Breaking changes

                - **ARCHI-38** Drop the v0 contract ([#43](https://github.com/Architrace-Intelligence/Architrace/pull/43))

                ## Features

                - **ARCHI-39** Add the Projects list ([#44](https://github.com/Architrace-Intelligence/Architrace/pull/44))

                ## Bug fixes

                - Keep the index page uncached

                ## Refactoring

                - **ARCHI-38** Drop the v0 contract ([#43](https://github.com/Architrace-Intelligence/Architrace/pull/43))

                ## Documentation

                - **ARCHI-40** Describe the release flow ([#45](https://github.com/Architrace-Intelligence/Architrace/pull/45))


                """
                    .trimIndent(),
            )
    }

    @Test
    fun `lists unknown conventional types under other changes and leaves references plain without a repository`() {
        val rendered = ReleaseNotes(null).render(listOf("style: Reindent (#7)"))

        assertThat(rendered).isEqualTo("## Other changes\n\n- Reindent (#7)\n\n")
    }

    @Test
    fun `says so when nothing conventional happened`() {
        assertThat(notes.render(listOf("Merge branch 'main'"))).isEqualTo("No conventional commits since the previous release.\n")
    }
}
