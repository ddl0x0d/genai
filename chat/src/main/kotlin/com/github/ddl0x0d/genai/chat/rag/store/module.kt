package com.github.ddl0x0d.genai.chat.rag.store

import com.github.ddl0x0d.genai.chat.rag.EmbeddingStoreType
import com.github.ddl0x0d.genai.chat.rag.RagConfig
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore
import io.qdrant.client.QdrantClient
import org.koin.dsl.module

val ragStoreModule = module {
    single<EmbeddingStore<TextSegment>> {
        val config: RagConfig = get()
        when (config.store.type) {
            EmbeddingStoreType.IN_MEMORY -> InMemoryEmbeddingStore()
            EmbeddingStoreType.CHROMA -> {
                val config: ChromaConfig = get()
                val connection: ChromaConnection = get()
                ChromaEmbeddingStore.builder()
                    .baseUrl(connection.baseUrl)
                    .collectionName(config.collection)
                    .build()
            }
            EmbeddingStoreType.PGVECTOR -> {
                val config: PgVectorConfig = get()
                val connection: PgVectorConnection = get()
                val embeddingModel: DimensionAwareEmbeddingModel = get()
                PgVectorEmbeddingStore.builder()
                    .host(connection.host)
                    .port(connection.port)
                    .user(connection.user)
                    .password(connection.password)
                    .database(connection.database)
                    .table(connection.table)
                    .dimension(embeddingModel.dimension())
                    .createTable(config.createTable)
                    .build()
            }
            EmbeddingStoreType.QDRANT -> {
                val config: QdrantConfig = get()
                val client: QdrantClient = get()
                QdrantEmbeddingStore.builder()
                    .collectionName(config.collection)
                    .client(client)
                    .build()
            }
        }
    }
}
