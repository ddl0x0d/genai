import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serial)
    alias(libs.plugins.kover)
    alias(libs.plugins.shadow)
    alias(libs.plugins.taskinfo)
    alias(libs.plugins.versions)
    application
}

dependencies {
    // CLI
    implementation(libs.clikt)
    // Configuration
    implementation(libs.hoplite.yaml)
    implementation(libs.kaml)
    // DI
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.logger)
    // GenAI
    implementation(platform(libs.langchain4j.bom))
    implementation(libs.langchain4j.embeddings.bge.small.en)
    implementation(libs.langchain4j.kotlin)
    implementation(libs.langchain4j.main)
    implementation(libs.langchain4j.model.anthropic)
    implementation(libs.langchain4j.model.gemini)
    implementation(libs.langchain4j.model.ollama)
    implementation(libs.langchain4j.model.openai)
    implementation(libs.langchain4j.parser.tika)
    implementation(libs.langchain4j.store.chroma)
    implementation(libs.langchain4j.store.pgvector)
    implementation(libs.langchain4j.store.qdrant)
    // Logging
    implementation(libs.kotlin.logging)
    runtimeOnly(libs.log4j.slf4j)
    runtimeOnly(libs.logback.classic)
    // Testcontainers
    implementation(platform(libs.testcontainers.bom))
    implementation(libs.testcontainers.chroma)
    implementation(libs.testcontainers.ollama)
    implementation(libs.testcontainers.postgres)
    implementation(libs.testcontainers.qdrant)
    // Testing
    testImplementation(libs.langchain4j.test)
    testImplementation(platform(libs.kotest.bom))
    testImplementation(libs.kotest.api)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.data)
    testImplementation(libs.kotest.junit5)
    testImplementation(libs.kotest.wiremock)
    testImplementation(libs.wiremock.kotlin)
    testImplementation(libs.wiremock.standalone)
}

detekt {
    config.from(rootProject.layout.projectDirectory.file("detekt.yml"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = JvmTarget.JVM_21
    }
}

kover {
    reports {
        filters {
            excludes {
                classes("com.github.ddl0x0d.genai.chat.MainKt")
                annotatedBy("kotlinx.serialization.Serializable")
            }
        }
    }
}

application {
    mainClass = "com.github.ddl0x0d.genai.chat.MainKt"
}

tasks {
    dependencyUpdates {
        gradleReleaseChannel = "current"
        rejectVersionIf {
            val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { candidate.version.uppercase().contains(it) }
            val isStable = stableKeyword || "^[0-9,.v-]+(-r)?$".toRegex().matches(candidate.version)
            isStable.not()
        }
    }
    test {
        useJUnitPlatform()
    }
    jar {
        manifest {
            attributes("Main-Class" to application.mainClass)
        }
    }
    shadowJar {
        dependsOn(check)
        isZip64 = true
        mergeServiceFiles()
    }
    withType<JavaExec> {
        standardInput = System.`in`
    }
    test.configure { finalizedBy(koverHtmlReport) }
}
