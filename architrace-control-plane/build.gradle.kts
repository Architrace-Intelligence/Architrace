plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.openapi.generator)
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

val uiBundle: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor(libs.spring.boot.configuration.processor)

    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.grpc.services)
    implementation(project(":api"))
    implementation(libs.swagger.ui)
    implementation(libs.webjars.locator.lite)
    implementation(libs.spring.grpc.spring.boot.starter)
    implementation(libs.spring.boot.starter.data.jdbc)
    implementation(libs.spring.boot.starter.liquibase)

    implementation(libs.postgresql)

    uiBundle(project(path = ":ui", configuration = "bundle"))

    testImplementation(libs.spring.boot.starter.actuator.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.data.jdbc.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.spring.grpc.test)
    testImplementation(libs.grpc.inprocess)

    testRuntimeOnly(libs.junit.platform.launcher)
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.grpc.bom.get().toString())
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
        all().forEach {
            it.plugins {
                create("grpc") {
                    option("@generated=omit")
                }
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
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
