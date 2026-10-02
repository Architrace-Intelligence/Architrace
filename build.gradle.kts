import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    `java-library`
    jacoco
    alias(libs.plugins.spotless) apply false
    alias(libs.plugins.sonarqube)
    alias(libs.plugins.graalvm.native)
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.protobuf) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.openapi.generator) apply false
    alias(libs.plugins.node) apply false
}

allprojects {
    group = property("group").toString()
    version = property("version").toString()

    repositories {
        mavenCentral()
    }

    dependencyLocking {
        lockAllConfigurations()
    }
}

fun Project.coverageMinimum(counter: String): java.math.BigDecimal =
    (findProperty("coverage.minimum.${counter.lowercase()}") ?: "0.85").toString().toBigDecimal()

val javaProjects = subprojects.filter { it.name != "ui" }

configure(javaProjects) {
    apply(plugin = "java-library")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "jacoco")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(property("javaVersion").toString().toInt())
        }
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("--enable-preview")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        jvmArgs("--enable-preview")
    }

    dependencies {
        testImplementation(rootProject.libs.junit.jupiter)
        testImplementation(rootProject.libs.assertj.core)
        testImplementation(rootProject.libs.mockito.core)
        testImplementation(rootProject.libs.mockito.junit.jupiter)
        testRuntimeOnly(rootProject.libs.junit.platform.launcher)
    }

    tasks.withType<Test> {
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.named<JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        classDirectories.setFrom(
            files(
                classDirectories.files.map {
                    fileTree(it) {
                        exclude("**/io/github/architrace/grpc/proto/**")
                        exclude("**/ControlPlaneApplication*")
                        exclude("**/io/github/architrace/control/plane/api/**")
                    }
                }
            )
        )
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        dependsOn(tasks.named("test"))
        classDirectories.setFrom(
            files(
                classDirectories.files.map {
                    fileTree(it) {
                        exclude("**/io/github/architrace/grpc/proto/**")
                        exclude("**/ControlPlaneApplication*")
                        exclude("**/io/github/architrace/control/plane/api/**")
                    }
                }
            )
        )
        violationRules {
            listOf("LINE", "BRANCH", "METHOD").forEach { counterName ->
                rule {
                    limit {
                        counter = counterName
                        value = "COVEREDRATIO"
                        minimum = coverageMinimum(counterName)
                    }
                }
            }
        }
    }

    extensions.configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            target("src/**/*.java")
            licenseHeaderFile(
                rootProject.file("license-header.txt"),
                "package "
            )
        }
    }
}

tasks.register<JavaExec>("runArchitrace") {
    group = "application"
    description = "Run Architrace CLI"
    classpath = project(":agent").sourceSets["main"].runtimeClasspath
    mainClass.set("io.github.architrace.MainApp")
    jvmArgs("--enable-preview")
}

tasks.register("buildRuntime") {
    group = "build"
    description = "Build runtime-related artifacts."
}

configurations.all {
    resolutionStrategy {
        force(libs.guava)
    }
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

graalvmNative {
    binaries {
        named("main") {
            imageName.set("architrace")
            mainClass.set("io.github.architrace.MainApp")
            buildArgs.add("--no-fallback")
        }
    }
}
