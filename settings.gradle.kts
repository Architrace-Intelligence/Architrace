pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "architrace"

include("architrace-api")
include("architrace-agent")
include("architrace-control-plane")
include("architrace-ui")
