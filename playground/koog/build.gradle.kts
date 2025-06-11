plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.koog.agents)
    implementation(libs.kotlin.logging)
    implementation(platform(libs.testcontainers.bom))
    implementation(libs.testcontainers.ollama)
    runtimeOnly(libs.logback.classic)
}
