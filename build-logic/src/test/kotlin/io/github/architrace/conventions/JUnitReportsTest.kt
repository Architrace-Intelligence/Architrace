/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.conventions

import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class JUnitReportsTest {
    @TempDir
    lateinit var directory: File

    @Test
    fun `sums a Gradle report with one suite and a Vitest report with nested suites`() {
        val gradle = report("TEST-Rules.xml", GRADLE_REPORT)
        val vitest = report("vitest.xml", VITEST_REPORT)

        assertThat(JUnitReports.counts(listOf(gradle, vitest)))
            .isEqualTo(TestCounts(tests = 13, failures = 1, errors = 1, skipped = 2))
    }

    @Test
    fun `treats missing attributes as zero and an empty list as no tests`() {
        val bare = report("bare.xml", """<testsuite name="bare"/>""")

        assertThat(JUnitReports.read(bare)).isEqualTo(TestCounts())
        assertThat(JUnitReports.counts(emptyList())).isEqualTo(TestCounts())
    }

    private fun report(name: String, content: String): File = directory.resolve(name).apply { writeText(content) }

    companion object {
        private const val GRADLE_REPORT =
            """<?xml version="1.0" encoding="UTF-8"?>
            <testsuite name="io.github.architrace.RulesTest" tests="7" skipped="1" failures="1" errors="0">
              <testcase name="reports" classname="RulesTest"/>
            </testsuite>"""

        private const val VITEST_REPORT =
            """<?xml version="1.0" encoding="UTF-8"?>
            <testsuites name="vitest tests" tests="6" failures="0" errors="1" time="1.2">
              <testsuite name="src/a.test.tsx" tests="4" failures="0" errors="1" skipped="1"/>
              <testsuite name="src/b.test.tsx" tests="2" failures="0" errors="0" skipped="0"/>
            </testsuites>"""
    }
}
