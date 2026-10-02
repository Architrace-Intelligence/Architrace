import com.github.gradle.node.npm.task.NpmTask

plugins {
    base
    id("com.github.node-gradle.node")
    id("com.diffplug.spotless")
}

description = "Architrace web UI"

node {
    download.set(true)
    version.set(property("nodeVersion").toString())
    workDir.set(rootProject.layout.projectDirectory.dir(".gradle/nodejs"))
    npmInstallCommand.set("ci")
}

val queryApiDocument =
    rootProject.file("architrace-api/src/main/resources/openapi/architrace-query-api.yaml")
val bundleDirectory = layout.buildDirectory.dir("dist")
val coverageDirectory = layout.buildDirectory.dir("coverage")

val uiSources = fileTree(projectDir) {
    include(
        "src/**",
        "public/**",
        "index.html",
        "vite.config.ts",
        "tsconfig*.json",
        "eslint.config.js",
        ".prettierrc.json",
        ".prettierignore",
        "package.json",
        "package-lock.json",
    )
    exclude("src/api/schema.d.ts")
}

val npmCheck by tasks.registering(NpmTask::class) {
    group = "verification"
    description = "Type-checks, lints, format-checks and tests the UI sources."
    dependsOn(tasks.named("npmInstall"))
    args.set(listOf("run", "check"))
    inputs.files(uiSources).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(queryApiDocument).withPathSensitivity(PathSensitivity.NONE)
    outputs.dir(coverageDirectory)
}

val npmBuild by tasks.registering(NpmTask::class) {
    group = "build"
    description = "Builds the production bundle of the UI."
    dependsOn(tasks.named("npmInstall"))
    args.set(listOf("run", "build"))
    inputs.files(uiSources).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(queryApiDocument).withPathSensitivity(PathSensitivity.NONE)
    outputs.dir(bundleDirectory)
}

configurations.consumable("bundle") {
    outgoing.artifact(bundleDirectory) { builtBy(npmBuild) }
}

tasks.register("test") {
    group = "verification"
    description = "Runs the UI quality gate, the counterpart of the Java test tasks."
    dependsOn(npmCheck)
}

tasks.check {
    dependsOn(npmCheck)
}

tasks.assemble {
    dependsOn(npmBuild)
}

spotless {
    format("web") {
        target("src/**/*.ts", "src/**/*.tsx", "src/**/*.css", "vite.config.ts", "eslint.config.js")
        targetExclude("src/api/schema.d.ts")
        licenseHeaderFile(rootProject.file("license-header.txt"), "^(?!(/\\*| \\*|\\*/|\\s*$)).+")
    }
}

sonar {
    properties {
        property("sonar.sources", "src")
        property("sonar.exclusions", "src/**/*.test.ts,src/**/*.test.tsx,src/test/**,src/api/schema.d.ts")
        property("sonar.tests", "src")
        property("sonar.test.inclusions", "src/**/*.test.ts,src/**/*.test.tsx")
        property("sonar.javascript.lcov.reportPaths", "build/coverage/lcov.info")
    }
}
