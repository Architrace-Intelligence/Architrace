/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.build

enum class Bump {
    NONE,
    PATCH,
    MINOR,
    MAJOR,
}

data class ConventionalCommit(
    val type: String?,
    val scope: String?,
    val breaking: Boolean,
    val subject: String,
) {
    val conventional: Boolean
        get() = type != null

    val bump: Bump
        get() =
            when {
                !conventional -> Bump.NONE
                breaking -> Bump.MAJOR
                type == "feat" -> Bump.MINOR
                type in PATCH_TYPES -> Bump.PATCH
                else -> Bump.NONE
            }

    companion object {
        private val HEADER = Regex("""^([a-z]+)(?:\(([^)]*)\))?(!)?:\s+(\S.*)$""")
        private val BREAKING_FOOTER = Regex("""^BREAKING[ -]CHANGE:""", RegexOption.MULTILINE)
        private val PATCH_TYPES = setOf("fix", "perf", "refactor", "build")

        fun parse(message: String): ConventionalCommit {
            val lines = message.trim().lines()
            val header = lines.first().trim()
            val body = lines.drop(1).joinToString("\n")
            val match = HEADER.matchEntire(header) ?: return ConventionalCommit(null, null, false, header)
            val (type, scope, bang, subject) = match.destructured
            return ConventionalCommit(
                type = type,
                scope = scope.ifEmpty { null },
                breaking = bang.isNotEmpty() || BREAKING_FOOTER.containsMatchIn(body),
                subject = subject.trim(),
            )
        }

        fun highestBump(commits: Iterable<ConventionalCommit>): Bump = commits.maxOfOrNull { it.bump } ?: Bump.NONE
    }
}
