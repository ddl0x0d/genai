package com.github.ddl0x0d.genai.chat.rag.store

import com.sksamuel.hoplite.ConfigBinder
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import org.koin.dsl.module
import org.koin.dsl.onClose
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.PostgreSQLContainer

private val logger = KotlinLogging.logger {}

private const val PREFIX = "rag.store.pgvector"

val pgVectorModule = module {
    single<PgVectorConfig> { get<ConfigBinder>().bindOrThrow(PREFIX) }
    single<PgVectorConnection> {
        val config: PgVectorConfig = get()
        when {
            config.host.isNotBlank() && config.port > 0 -> PgVectorConnection.External(config)
            config.imageTag.isNotBlank() -> PgVectorConnection.TestContainers(config)
            else -> error("Either $PREFIX.host or $PREFIX.image-tag must be specified")
        }
    }.onClose { connection ->
        if (connection is PgVectorConnection.TestContainers) {
            connection.pgVector.close()
        }
    }
}

@Serializable
data class PgVectorConfig(
    override val host: String,
    override val port: Int,
    override val user: String,
    override val password: String,
    override val database: String,
    override val table: String,
    val createTable: Boolean,
    val imageTag: String,
    val mountPath: String? = null,
) : PgVectorConnection

sealed interface PgVectorConnection {

    val host: String
    val port: Int
    val user: String
    val password: String
    val database: String
    val table: String

    data class External(val config: PgVectorConfig) : PgVectorConnection by config

    data class TestContainers(private val config: PgVectorConfig) : PgVectorConnection {

        val pgVector: PostgreSQLContainer<*> = PostgreSQLContainer("pgvector/pgvector:${config.imageTag}")
            .withLogConsumer { logger.debug { it.utf8StringWithoutLineEnding } }
            .apply {
                config.mountPath
                    ?.replace("~", System.getProperty("user.home"))
                    ?.let { path -> withFileSystemBind(path, "/data", BindMode.READ_WRITE) }
            }

        init {
            pgVector.start()
        }

        override val host: String
            get() = pgVector.host

        override val port: Int
            get() = pgVector.firstMappedPort

        override val user: String
            get() = pgVector.username

        override val password: String
            get() = pgVector.password

        override val database: String
            get() = pgVector.databaseName

        override val table: String = config.table
    }
}
