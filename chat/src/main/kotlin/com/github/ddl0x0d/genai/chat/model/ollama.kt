package com.github.ddl0x0d.genai.chat.model

import com.github.ddl0x0d.genai.chat.Initializer
import com.sksamuel.hoplite.ConfigBinder
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose
import org.testcontainers.containers.BindMode
import org.testcontainers.ollama.OllamaContainer

private val logger = KotlinLogging.logger {}

private const val PREFIX = "model.ollama"

val ollamaModule = module {
    single<OllamaConfig> { get<ConfigBinder>().bindOrThrow(PREFIX) }
    singleOf(::OllamaConfigValidator).bind(Initializer::class)
    single<OllamaConnection> {
        val config: OllamaConfig = get()
        when {
            config.baseUrl.isNotBlank() -> OllamaConnection.External(config.baseUrl)
            config.imageTag.isNotBlank() -> OllamaConnection.TestContainers(config)
            else -> error("Either $PREFIX.base-url or $PREFIX.image-tag must be specified")
        }
    }.onClose { connection ->
        if (connection is OllamaConnection.TestContainers) {
            connection.ollama.close()
        }
    }
}

@Serializable
data class OllamaConfig(
    val baseUrl: String,
    val imageTag: String,
    val mountPath: String? = null,
    val chatModel: String,
    val embeddingModel: String,
) {
    val models: List<String> get() = listOf(chatModel, embeddingModel)
}

class OllamaConfigValidator(private val config: OllamaConfig) : Initializer {
    override fun init() {
        require(config.baseUrl.isNotBlank() || config.imageTag.isNotBlank()) {
            "Configuration error: either '$PREFIX.base-url' or '$PREFIX.image-tag' must be specified."
        }
    }
}

sealed interface OllamaConnection {

    val baseUrl: String

    data class External(override val baseUrl: String) : OllamaConnection

    data class TestContainers(private val config: OllamaConfig) : OllamaConnection {

        val ollama: OllamaContainer = OllamaContainer("ollama/ollama:${config.imageTag}")
            .withLogConsumer { logger.debug { it.utf8StringWithoutLineEnding } }
            .apply {
                config.mountPath
                    ?.replace("~", System.getProperty("user.home"))
                    ?.let { path -> withFileSystemBind(path, "/root/.ollama", BindMode.READ_WRITE) }
            }

        init {
            ollama.start()
            val ollamaModels: Set<String> = ollama.execInContainer(PREFIX, "ls")
                .stdout.lines().drop(1).map { it.substringBefore(" ") }.toSet()
            config.models.filter { it !in ollamaModels }.forEach { model ->
                logger.info { "Downloading Ollama model $model" }
                ollama.execInContainer(PREFIX, "pull", model)
            }
        }

        override val baseUrl: String
            get() = ollama.endpoint
    }
}
