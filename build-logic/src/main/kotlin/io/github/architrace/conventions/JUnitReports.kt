/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.conventions

import java.io.File
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

object JUnitReports {
    fun counts(reports: Iterable<File>): TestCounts = reports.map(::read).fold(TestCounts(), TestCounts::plus)

    fun read(report: File): TestCounts {
        val document = secureFactory().newDocumentBuilder().parse(report)
        val root = document.documentElement
        val suites = if (root.tagName == "testsuite") listOf(root) else root.elements("testsuite")
        return suites.map(::countsOf).fold(TestCounts(), TestCounts::plus)
    }

    private fun countsOf(suite: Element): TestCounts =
        TestCounts(
            suite.number("tests"),
            suite.number("failures"),
            suite.number("errors"),
            suite.number("skipped"),
        )

    private fun Element.number(attribute: String): Int = getAttribute(attribute).toIntOrNull() ?: 0

    private fun Element.elements(tag: String): List<Element> {
        val nodes = getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun secureFactory(): DocumentBuilderFactory =
        DocumentBuilderFactory.newInstance().apply {
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }
}
