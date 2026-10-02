plugins {
    base
    id("architrace.versioning")
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.dependency.check)
}

allprojects {
    group = property("group").toString()
    version = rootProject.version

    repositories {
        mavenCentral()
    }

    dependencyLocking {
        lockAllConfigurations()
    }
}

val buildLogic = gradle.includedBuild("build-logic")

tasks.register("test") {
    group = "verification"
    description = "Runs the tests of the build logic; the module test tasks run alongside by name."
    dependsOn(buildLogic.task(":test"))
}

tasks.register("spotlessCheck") {
    group = "verification"
    description = "Checks the formatting of the build logic; the module tasks run alongside by name."
    dependsOn(buildLogic.task(":spotlessCheck"))
}

tasks.register("spotlessApply") {
    group = "formatting"
    description = "Formats the build logic; the module tasks run alongside by name."
    dependsOn(buildLogic.task(":spotlessApply"))
}

tasks.check {
    dependsOn(buildLogic.task(":check"))
}

sonar {
    properties {
        property("sonar.projectKey", "Architrace-Intelligence_Architrace-agent")
        property("sonar.organization", "architrace-intelligence")
        property("sonar.host.url", "https://sonarcloud.io")
        property(
            "sonar.coverage.exclusions",
            "**/io/github/architrace/grpc/proto/**,**/io/github/architrace/control/plane/api/**"
        )
        property("sonar.exclusions", "**/build/generated/**")
    }
}

dependencyCheck {
    failBuildOnCVSS = 7.0f
    scanConfigurations = listOf("runtimeClasspath")
    formats = listOf("HTML", "SARIF")
    outputDirectory = layout.buildDirectory.dir("reports/dependency-check")
    suppressionFile = "config/dependency-check/suppressions.xml"
    nvd.apiKey = providers.environmentVariable("NVD_API_KEY").orNull
}
