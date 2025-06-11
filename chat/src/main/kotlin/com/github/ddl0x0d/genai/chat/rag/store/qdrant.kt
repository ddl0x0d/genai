package com.github.ddl0x0d.genai.chat.rag.store

import com.github.ddl0x0d.genai.chat.Initializer
import com.sksamuel.hoplite.ConfigBinder
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel
import io.github.oshai.kotlinlogging.KotlinLogging
import io.qdrant.client.QdrantClient
import io.qdrant.client.QdrantGrpcClient
import io.qdrant.client.grpc.Collections
import kotlinx.serialization.Serializable
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose
import org.testcontainers.containers.BindMode
import org.testcontainers.qdrant.QdrantContainer

private val logger = KotlinLogging.logger {}

private const val PREFIX = "rag.store.qdrant"

val qdrantModule = module {
    single<QdrantConfig> { get<ConfigBinder>().bindOrThrow(PREFIX) }
    single<QdrantConnection> {
        val config: QdrantConfig = get()
        when {
            config.host.isNotBlank() && config.port > 0 -> QdrantConnection.External(config.host, config.port)
            config.imageTag.isNotBlank() -> QdrantConnection.TestContainers(config)
            else -> error("Either $PREFIX.host or $PREFIX.image-tag must be specified")
        }
    }.onClose { connection ->
        if (connection is QdrantConnection.TestContainers) {
            connection.qdrant.close()
        }
    }
    single<QdrantClient> {
        val connection: QdrantConnection = get()
        QdrantClient(QdrantGrpcClient.newBuilder(connection.host, connection.port, false).build())
    }
    singleOf(::QdrantInitializer).bind(Initializer::class)
}

@Serializable
data class QdrantConfig(
    val host: String,
    val port: Int,
    val imageTag: String,
    val mountPath: String? = null,
    val collection: String,
)

sealed interface QdrantConnection {

    val host: String
    val port: Int

    data class External(
        override val host: String,
        override val port: Int,
    ) : QdrantConnection

    data class TestContainers(private val config: QdrantConfig) : QdrantConnection {

        val qdrant: QdrantContainer = QdrantContainer("qdrant/qdrant:${config.imageTag}")
            .withLogConsumer { logger.debug { it.utf8StringWithoutLineEnding } }
            .apply {
                config.mountPath
                    ?.replace("~", System.getProperty("user.home"))
                    ?.let { path -> withFileSystemBind(path, "/data", BindMode.READ_WRITE) }
            }

        init {
            qdrant.start()
        }

        override val host: String
            get() = qdrant.host

        override val port: Int
            get() = qdrant.grpcPort
    }
}

class QdrantInitializer(
    val client: QdrantClient,
    val config: QdrantConfig,
    val embeddingModel: DimensionAwareEmbeddingModel,
) : Initializer {
    override fun init() {
        logger.info { "Checking if collection '${config.collection}' exists..." }
        val exists: Boolean = client.collectionExistsAsync(config.collection).get()
        if (exists) {
            logger.info { "Collection '${config.collection}' already exists" }
        } else {
            logger.info { "Collection '${config.collection}' does not exist, creating it..." }
            val params: Collections.VectorParams = Collections.VectorParams.newBuilder()
                .setSize(embeddingModel.dimension().toLong())
                .setDistance(Collections.Distance.Cosine)
                .build()
            client.createCollectionAsync(config.collection, params).get()
            logger.info { "Collection '${config.collection}' created" }
        }
    }
}
