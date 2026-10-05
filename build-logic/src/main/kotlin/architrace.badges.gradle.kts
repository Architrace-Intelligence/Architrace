import io.github.architrace.conventions.Badge
import io.github.architrace.conventions.JUnitReports

val reports =
    fileTree(rootDir) {
        include("**/build/test-results/**/*.xml")
        exclude("**/node_modules/**", "**/.gradle/**", "**/dist/**")
    }

tasks.register("writeBadges") {
    group = "reporting"
    description = "Writes build/badges/tests.json and version.json (shields.io endpoint badges) from every JUnit report."
    val resolvedVersion = version.toString()
    val output = layout.buildDirectory.dir("badges")
    inputs.files(reports).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.property("version", resolvedVersion)
    outputs.dir(output)
    doLast {
        val counts = JUnitReports.counts(reports.files)
        val directory = output.get().asFile.apply { mkdirs() }
        directory.resolve("tests.json").writeText(Badge.tests(counts).toJson() + "\n")
        directory.resolve("version.json").writeText(Badge.version(resolvedVersion).toJson() + "\n")
        println("${reports.files.size} reports: ${counts.passed} passed, ${counts.failed} failed, ${counts.skipped} skipped")
    }
}
