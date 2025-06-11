import org.springframework.boot.gradle.plugin.SpringBootPlugin

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serial)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kover)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.taskinfo)
    alias(libs.plugins.versions)
    application
}

dependencies {
    // Spring AI
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
    implementation(platform(libs.spring.ai.bom))
    implementation(libs.spring.ai.model.ollama)
//    implementation(libs.spring.ai.model.openai)
    implementation(libs.spring.ai.docker)
    implementation(libs.spring.ai.mcp.client.core)
    // Spring Shell
    implementation(platform(libs.spring.shell.bom))
    implementation(libs.spring.shell.jna)
    // Logging
    implementation(libs.kotlin.logging)
    runtimeOnly(libs.logback.classic)
    // Testing
    testImplementation(libs.spring.ai.testcontainers)
    testImplementation(libs.testcontainers.ollama)
}

detekt {
    config.from(rootProject.layout.projectDirectory.file("detekt.yml"))
}
