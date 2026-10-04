import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("architrace.java")
    application
    id("com.gradleup.shadow")
    id("com.google.protobuf")
}

description = "Architrace agent"

dependencies {
    annotationProcessor(libs.picocli.codegen)
    compileOnly(libs.javax.annotation.api)

    implementation(libs.guice)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.grpc.netty.shaded)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.services)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.util)
    implementation(libs.opentelemetry.proto)
    implementation(libs.picocli)
    implementation(libs.protobuf.java)
    implementation(libs.slf4j.api)
    implementation(project(":architrace-api"))

    runtimeOnly(libs.logback.classic)

    testImplementation(libs.awaitility)
    testImplementation(libs.logback.classic)
}

application {
    mainClass.set("io.github.architrace.MainApp")
    applicationDefaultJvmArgs = listOf("--enable-preview")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-Aproject=${project.group}/${project.name}"))
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("all")
    mergeServiceFiles()
    manifest {
        attributes("Main-Class" to application.mainClass.get())
    }
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    plugins {
        create("grpc") {
            artifact = libs.grpc.protoc.gen.java.get().toString()
        }
    }
    generateProtoTasks {
        all().forEach { task: com.google.protobuf.gradle.GenerateProtoTask ->
            task.plugins {
                maybeCreate("grpc")
            }
        }
    }
}

tasks.named("build") {
    dependsOn("shadowJar")
}
