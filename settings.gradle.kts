dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

rootProject.name = "genai"

include(":chat")
include(":playground:koog")
include(":playground:spring-ai")
