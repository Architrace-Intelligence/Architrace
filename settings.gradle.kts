rootProject.name = "architrace"

include("api")
include("agent")
include("control-plane")

project(":api").projectDir = file("architrace-api")
project(":agent").projectDir = file("architrace-agent")
project(":control-plane").projectDir = file("architrace-control-plane")
