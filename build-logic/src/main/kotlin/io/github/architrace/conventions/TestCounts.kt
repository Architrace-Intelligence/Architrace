/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.conventions

data class TestCounts(
    val tests: Int = 0,
    val failures: Int = 0,
    val errors: Int = 0,
    val skipped: Int = 0,
) {
    val failed: Int
        get() = failures + errors

    val passed: Int
        get() = tests - failed - skipped

    operator fun plus(other: TestCounts): TestCounts =
        TestCounts(
            tests + other.tests,
            failures + other.failures,
            errors + other.errors,
            skipped + other.skipped,
        )
}
