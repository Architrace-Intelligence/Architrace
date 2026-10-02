rootProject.name = "architrace"

include("api")
include("agent")
include("control-plane")
include("ui")

project(":api").projectDir = file("architrace-api")
project(":agent").projectDir = file("architrace-agent")
project(":control-plane").projectDir = file("architrace-control-plane")
project(":ui").projectDir = file("architrace-ui")
