plugins {
    id("architrace.java")
    id("com.google.protobuf")
}

description = "Architrace contracts: protobuf for agents, OpenAPI for the Query API"

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
                maybeCreate("grpc")
            }
        }
    }
}
