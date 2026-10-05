/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.conventions

data class Badge(val label: String, val message: String, val color: String) {
    fun toJson(): String =
        """{"schemaVersion":1,"label":"${escape(label)}","message":"${escape(message)}","color":"${escape(color)}"}"""

    private fun escape(text: String): String = text.replace("\\", "\\\\").replace("\"", "\\\"")

    companion object {
        fun tests(counts: TestCounts): Badge {
            val parts = buildList {
                add("${counts.passed} passed")
                if (counts.failed > 0) add("${counts.failed} failed")
                if (counts.skipped > 0) add("${counts.skipped} skipped")
            }
            val color =
                when {
                    counts.failed > 0 -> "red"
                    counts.skipped > 0 -> "yellow"
                    else -> "brightgreen"
                }
            return Badge("tests", parts.joinToString(", "), color)
        }

        fun version(version: String): Badge =
            Badge("version", version, if (version.endsWith("-SNAPSHOT")) "orange" else "blue")
    }
}
