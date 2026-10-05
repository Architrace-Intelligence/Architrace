plugins {
    id("architrace.spring-boot")
    id("com.google.protobuf")
    id("org.openapi.generator")
}

description = "Architrace control plane"

val queryApiDocument =
    rootProject.file("architrace-api/src/main/resources/openapi/architrace-query-api.yaml")
val generatedQueryApi = layout.buildDirectory.dir("generated/openapi")

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

val uiBundle: Configuration = configurations.create("uiBundle") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

dependencies {
    annotationProcessor(libs.spring.boot.configuration.processor)

    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.grpc.services)
    implementation(project(":architrace-api"))
    implementation(libs.swagger.ui)
    implementation(libs.webjars.locator.lite)
    implementation(libs.spring.boot.starter.grpc.server)
    implementation(libs.spring.boot.starter.data.jdbc)
    implementation(libs.spring.boot.starter.liquibase)

    implementation(libs.postgresql)

    uiBundle(project(path = ":architrace-ui", configuration = "bundle"))

    testImplementation(libs.spring.boot.starter.actuator.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.data.jdbc.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.grpc.inprocess)

    testRuntimeOnly(libs.junit.platform.launcher)
}

extra["grpc-java.version"] = libs.versions.grpc.get()
extra["jackson-2-bom.version"] = libs.versions.jackson.get()
extra["jackson-bom.version"] = libs.versions.jackson3.get()
extra["netty.version"] = libs.versions.netty.get()
extra["protobuf-java.version"] = libs.versions.protobuf.asProvider().get()
extra["tomcat.version"] = libs.versions.tomcat.get()

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
        all().forEach {
            it.plugins {
                maybeCreate("grpc").apply {
                    if (options.isEmpty()) {
                        option("@generated=omit")
                    }
                }
            }
        }
    }
}

openApiValidate {
    inputSpec.set(queryApiDocument.path)
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set(queryApiDocument.path)
    outputDir.set(generatedQueryApi.get().asFile.path)
    apiPackage.set("io.github.architrace.control.plane.api")
    modelPackage.set("io.github.architrace.control.plane.api.model")
    modelNameSuffix.set("Dto")
    globalProperties.set(mapOf("apis" to "", "models" to ""))
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "skipDefaultInterface" to "true",
            "useTags" to "true",
            "useSpringBoot3" to "true",
            "useResponseEntity" to "false",
            "useBeanValidation" to "false",
            "openApiNullable" to "false",
            "documentationProvider" to "none",
            "annotationLibrary" to "none",
            "dateLibrary" to "java8",
            "hideGenerationTimestamp" to "true",
        )
    )
}

sourceSets {
    main {
        java.srcDir(generatedQueryApi.map { it.dir("src/main/java") })
    }
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}

tasks.processResources {
    from(uiBundle) {
        into("static")
    }
}

tasks.named("check") {
    dependsOn(tasks.openApiValidate)
}
