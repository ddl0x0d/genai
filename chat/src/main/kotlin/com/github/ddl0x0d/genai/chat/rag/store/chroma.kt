package com.github.ddl0x0d.genai.chat.rag.store

import com.sksamuel.hoplite.ConfigBinder
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import org.koin.dsl.module
import org.koin.dsl.onClose
import org.testcontainers.chromadb.ChromaDBContainer
import org.testcontainers.containers.BindMode

private val logger = KotlinLogging.logger {}

private const val PREFIX = "rag.store.chroma"

val chromaModule = module {
    single<ChromaConfig> { get<ConfigBinder>().bindOrThrow(PREFIX) }
    single<ChromaConnection> {
        val config: ChromaConfig = get()
        when {
            config.host.isNotBlank() && config.port > 0 -> ChromaConnection.External(config)
            config.imageTag.isNotBlank() -> ChromaConnection.TestContainers(config)
            else -> error("Either $PREFIX.base-url or $PREFIX.image-tag must be specified")
        }
    }.onClose { connection ->
        if (connection is ChromaConnection.TestContainers) {
            connection.chroma.close()
        }
    }
}

@Serializable
data class ChromaConfig(
    val host: String,
    val port: Int,
    val imageTag: String,
    val mountPath: String? = null,
    val collection: String,
)

sealed interface ChromaConnection {

    val baseUrl: String

    data class External(val config: ChromaConfig) : ChromaConnection {
        override val baseUrl: String = "http://${config.host}:${config.port}"
    }

    data class TestContainers(private val config: ChromaConfig) : ChromaConnection {

        val chroma: ChromaDBContainer = ChromaDBContainer("chromadb/chroma:${config.imageTag}")
            .withLogConsumer { logger.debug { it.utf8StringWithoutLineEnding } }
            .apply {
                config.mountPath
                    ?.replace("~", System.getProperty("user.home"))
                    ?.let { path -> withFileSystemBind(path, "/data", BindMode.READ_WRITE) }
            }

        init {
            chroma.start()
        }

        override val baseUrl: String
            get() = chroma.endpoint
    }
}
