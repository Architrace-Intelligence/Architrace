/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.conventions

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BadgeTest {
    @Test
    fun `counts only the passed tests when nothing failed or was skipped`() {
        assertThat(Badge.tests(TestCounts(tests = 354)))
            .isEqualTo(Badge("tests", "354 passed", "brightgreen"))
    }

    @Test
    fun `names the failed and skipped tests and turns red on a failure`() {
        assertThat(Badge.tests(TestCounts(tests = 10, failures = 1, errors = 1, skipped = 2)))
            .isEqualTo(Badge("tests", "6 passed, 2 failed, 2 skipped", "red"))
        assertThat(Badge.tests(TestCounts(tests = 10, skipped = 1)).color).isEqualTo("yellow")
    }

    @Test
    fun `colours a snapshot version orange and a release blue`() {
        assertThat(Badge.version("0.1.0-abc1234-SNAPSHOT").color).isEqualTo("orange")
        assertThat(Badge.version("0.1.0").color).isEqualTo("blue")
    }

    @Test
    fun `renders the shields endpoint schema with escaped quotes`() {
        assertThat(Badge("tests", "say \"hi\"", "blue").toJson())
            .isEqualTo("""{"schemaVersion":1,"label":"tests","message":"say \"hi\"","color":"blue"}""")
    }
}
