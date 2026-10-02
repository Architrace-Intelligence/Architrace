/*
 * SPDX-FileCopyrightText: Copyright (c) 2026 Dmytro Hryshchenko
 * SPDX-License-Identifier: Apache-2.0
 */

package io.github.architrace.build

import java.io.File
import java.io.IOException
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

class GitHistory(private val directory: File) {

    fun hasTag(tag: String): Boolean = withRepository(false) { it.resolve(tagRef(tag)) != null }

    fun commitsSince(tag: String): List<String> =
        withRepository(emptyList()) { repository ->
            val head = repository.resolve(Constants.HEAD) ?: return@withRepository emptyList()
            val base = repository.resolve("${tagRef(tag)}^{commit}")
            Git(repository).use { git ->
                val log = git.log()
                if (base == null) log.add(head) else log.addRange(base, head)
                log.call().map { it.fullMessage.trim() }
            }
        }

    fun remoteUrl(): String? =
        withRepository(null) { repository ->
            repository.config.getString("remote", "origin", "url")?.let(::toWebUrl)
        }

    private fun <T> withRepository(fallback: T, action: (Repository) -> T): T {
        val repository =
            try {
                FileRepositoryBuilder().readEnvironment().findGitDir(directory).build()
            } catch (_: IllegalArgumentException) {
                return fallback
            } catch (_: IOException) {
                return fallback
            }
        return repository.use(action)
    }

    private fun tagRef(tag: String) = "${Constants.R_TAGS}$tag"

    companion object {
        private val REMOTE = Regex("""^(?:ssh://)?(?:git@|https://)([^:/]+)[:/](.+?)(?:\.git)?/?$""")

        fun toWebUrl(remote: String): String? =
            REMOTE.matchEntire(remote.trim())?.let { "https://${it.groupValues[1]}/${it.groupValues[2]}" }
    }
}
