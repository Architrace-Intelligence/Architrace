import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
    alias(libs.plugins.spotless)
}

description = "Gradle convention plugins of the Architrace build"

java {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_24)
    }
}

dependencyLocking {
    lockAllConfigurations()
}

dependencies {
    implementation(plugin(libs.plugins.spotless))
    implementation(plugin(libs.plugins.sonarqube))
    implementation(plugin(libs.plugins.shadow))
    implementation(plugin(libs.plugins.protobuf))
    implementation(plugin(libs.plugins.spring.boot))
    implementation(plugin(libs.plugins.spring.dependency.management))
    implementation(plugin(libs.plugins.openapi.generator))
    implementation(plugin(libs.plugins.node))
    implementation(plugin(libs.plugins.axion.release))
    implementation(libs.jgit)
    implementation(libs.java.semver)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

fun plugin(dependency: Provider<PluginDependency>): Provider<String> =
    dependency.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }

tasks.test {
    useJUnitPlatform()
}

spotless {
    kotlin {
        target("src/**/*.kt")
        licenseHeaderFile(file("../license-header.txt"), "(package |@file)")
    }
}
