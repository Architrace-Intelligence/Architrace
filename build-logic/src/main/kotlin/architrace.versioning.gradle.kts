import io.github.architrace.conventions.ConventionalCommitsIncrementer
import io.github.architrace.conventions.GitHistory
import io.github.architrace.conventions.ReleaseNotes
import pl.allegro.tech.build.axion.release.domain.properties.VersionProperties

plugins {
    id("pl.allegro.tech.build.axion-release")
}

val tagPrefix = "v"
val history = GitHistory(rootDir)

scmVersion {
    tag {
        prefix.set(tagPrefix)
        versionSeparator.set("")
    }
    useHighestVersion.set(true)
    versionIncrementer(ConventionalCommitsIncrementer(history, tagPrefix))
    versionCreator(
        VersionProperties.Creator { versionFromTag, position ->
            if (position.isSnapshot || !position.isClean) {
                "$versionFromTag-${position.shortRevision}"
            } else {
                versionFromTag
            }
        }
    )
}

version = scmVersion.version

tasks.register("printVersion") {
    group = "versioning"
    description = "Prints the version of this checkout: X.Y.Z on a release tag, X.Y.Z-<sha>-SNAPSHOT otherwise."
    val resolved = version.toString()
    doLast { println(resolved) }
}

tasks.register("printReleaseVersion") {
    group = "versioning"
    description = "Prints the version a release from this commit would get; equal to the latest tag when nothing releasable happened."
    val resolved = scmVersion.undecoratedVersion
    doLast { println(resolved) }
}

tasks.register("releaseNotes") {
    group = "versioning"
    description = "Writes build/release-notes.md from the Conventional Commits since the previous release tag (override with -PreleaseNotes.since=<tag>)."
    val since = providers.gradleProperty("releaseNotes.since").orElse("$tagPrefix${scmVersion.previousVersion}")
    val output = layout.buildDirectory.file("release-notes.md")
    outputs.file(output)
    doLast {
        val notes = ReleaseNotes(history.remoteUrl()).render(history.commitsSince(since.get()))
        output.get().asFile.writeText(notes)
        println(notes)
    }
}
