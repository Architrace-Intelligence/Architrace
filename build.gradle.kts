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
}

allprojects {
    group = property("group").toString()
    version = property("version").toString()

    repositories {
        mavenCentral()
        maven("https://repo.spring.io/milestone")
        maven("https://repo.spring.io/snapshot")
    }
}

subprojects {
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
                    }
                }
            )
        )
        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = "0.85".toBigDecimal()
                }
            }
            rule {
                limit {
                    counter = "BRANCH"
                    value = "COVEREDRATIO"
                    minimum = "0.85".toBigDecimal()
                }
            }
            rule {
                limit {
                    counter = "METHOD"
                    value = "COVEREDRATIO"
                    minimum = "0.85".toBigDecimal()
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
        property("sonar.coverage.exclusions", "**/io/github/architrace/grpc/proto/**")
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
