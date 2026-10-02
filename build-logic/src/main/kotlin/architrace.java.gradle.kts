import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    `java-library`
    jacoco
    checkstyle
    id("com.diffplug.spotless")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val formatJava = (findProperty("java.format.enabled") ?: "true").toString().toBoolean()
val coverageExclusions =
    listOf(
        "**/io/github/architrace/grpc/proto/**",
        "**/ControlPlaneApplication*",
        "**/io/github/architrace/control/plane/api/**",
    )

fun coverageMinimum(counter: String) =
    (findProperty("coverage.minimum.${counter.lowercase()}") ?: "0.85").toString().toBigDecimal()

fun coverageClasses(directories: Iterable<File>) =
    files(directories.map { fileTree(it) { exclude(coverageExclusions) } })

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(property("javaVersion").toString().toInt())
    }
}

dependencies {
    testImplementation(libs.findLibrary("junit-jupiter").get())
    testImplementation(libs.findLibrary("assertj-core").get())
    testImplementation(libs.findLibrary("mockito-core").get())
    testImplementation(libs.findLibrary("mockito-junit-jupiter").get())
    testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("--enable-preview")
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-preview")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
        )
    }
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    classDirectories.setFrom(coverageClasses(classDirectories.files))
    reports {
        xml.required = true
        html.required = true
    }
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.named("test"))
    classDirectories.setFrom(coverageClasses(classDirectories.files))
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

tasks.check {
    dependsOn(tasks.named("jacocoTestCoverageVerification"))
}

spotless {
    java {
        target("src/**/*.java")
        if (formatJava) {
            palantirJavaFormat(libs.findVersion("palantir-java-format").get().toString())
        } else {
            importOrder("\\#", "")
        }
        licenseHeaderFile(rootProject.file("license-header.txt"), "package ")
    }
}

checkstyle {
    toolVersion = libs.findVersion("checkstyle").get().toString()
    configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    maxWarnings = 0
}

tasks.withType<Checkstyle>().configureEach {
    val generatedSources = layout.buildDirectory.get().asFile.toPath()
    exclude { it.file.toPath().startsWith(generatedSources) }
    reports {
        xml.required = true
        html.required = false
    }
}
