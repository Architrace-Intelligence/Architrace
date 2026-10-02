plugins {
    id("architrace.java")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

springBoot {
    buildInfo {
        excludes.set(setOf("time"))
    }
}

tasks.named<Jar>("jar") {
    enabled = false
}
