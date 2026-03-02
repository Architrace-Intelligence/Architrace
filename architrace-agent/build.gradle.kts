import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    application
    alias(libs.plugins.shadow)
    alias(libs.plugins.protobuf)
}

dependencies {
    implementation(libs.guice)
    implementation(libs.slf4j.api)
    implementation(libs.picocli)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.grpc.netty.shaded)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.services)
    implementation(libs.grpc.util)
    implementation(libs.protobuf.java)
    implementation(libs.opentelemetry.proto)
    compileOnly(libs.javax.annotation.api)
    testImplementation(libs.logback.classic)
    annotationProcessor(libs.picocli.codegen)
    runtimeOnly(libs.logback.classic)
    implementation(project(":api"))
}

application {
    mainClass.set("io.github.architrace.MainApp")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf(
        "-Aproject=${project.group}/${project.name}"
    ))
}

tasks.withType<JavaExec> {
    jvmArgs("--enable-preview")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("all")
    mergeServiceFiles()
    manifest {
        attributes(
            "Main-Class" to application.mainClass.get()
        )
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
                create("grpc")
            }
        }
    }
}

tasks.named("build") {
    dependsOn("shadowJar")
}
