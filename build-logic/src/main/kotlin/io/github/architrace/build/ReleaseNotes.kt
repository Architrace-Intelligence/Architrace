/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.build

class ReleaseNotes(private val repositoryUrl: String?) {

    fun render(messages: List<String>): String {
        val commits = messages.map(ConventionalCommit::parse).filter { it.conventional }
        if (commits.isEmpty()) {
            return "No conventional commits since the previous release.\n"
        }
        val notes = StringBuilder()
        val breaking = commits.filter { it.breaking }
        if (breaking.isNotEmpty()) {
            notes.section("Breaking changes", breaking)
        }
        SECTIONS.forEach { (type, title) ->
            val ofType = commits.filter { it.type == type }
            if (ofType.isNotEmpty()) {
                notes.section(title, ofType)
            }
        }
        val other = commits.filter { commit -> SECTIONS.none { it.first == commit.type } }
        if (other.isNotEmpty()) {
            notes.section("Other changes", other)
        }
        return notes.toString()
    }

    private fun StringBuilder.section(title: String, commits: List<ConventionalCommit>) {
        append("## ").append(title).append("\n\n")
        commits.forEach { append("- ").append(line(it)).append('\n') }
        append('\n')
    }

    private fun line(commit: ConventionalCommit): String {
        val scope = commit.scope?.let { "**$it** " } ?: ""
        return scope + linkPullRequests(commit.subject)
    }

    private fun linkPullRequests(text: String): String =
        if (repositoryUrl == null) {
            text
        } else {
            PULL_REQUEST.replace(text) { "[#${it.groupValues[1]}]($repositoryUrl/pull/${it.groupValues[1]})" }
        }

    companion object {
        private val PULL_REQUEST = Regex("""#(\d+)""")
        private val SECTIONS =
            listOf(
                "feat" to "Features",
                "fix" to "Bug fixes",
                "perf" to "Performance",
                "refactor" to "Refactoring",
                "build" to "Build",
                "ci" to "Continuous integration",
                "docs" to "Documentation",
                "test" to "Tests",
                "chore" to "Chores",
            )
    }
}
