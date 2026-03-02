plugins {
    `java-library`
    alias(libs.plugins.protobuf)
}

dependencies {
    api(libs.grpc.stub)
    api(libs.grpc.protobuf)
    api(libs.protobuf.java)
    api(libs.jackson.databind)
    api(libs.jakarta.validation.api)
    api(libs.swagger.annotations)
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
                create("grpc")
            }
        }
    }
}
