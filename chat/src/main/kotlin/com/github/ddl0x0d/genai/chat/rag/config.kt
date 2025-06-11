package com.github.ddl0x0d.genai.chat.rag

import com.github.ddl0x0d.genai.chat.model.ChatModelType
import kotlinx.serialization.Serializable

@Serializable
data class RagConfig(
    val embedding: Embedding,
    val ingestion: Ingestion,
    val retrieval: Retrieval,
    val router: Router,
    val store: Store,
) {

    @Serializable
    data class Embedding(
        val model: EmbeddingModelType,
    )

    @Serializable
    data class Ingestion(
        val chunkSize: Int,
        val maxSegmentSize: Int,
        val logDuration: Boolean,
    )

    @Serializable
    data class Retrieval(
        val maxResults: Int,
        val minScore: Double,
    )

    @Serializable
    data class Router(
        val enabled: Boolean,
        val model: ChatModelType,
    )

    @Serializable
    data class Store(
        val type: EmbeddingStoreType,
    )
}

enum class EmbeddingModelType {
    BGE_SMALL_EN,
    GEMINI,
    OLLAMA,
    OPENAI,
}

enum class EmbeddingStoreType {
    IN_MEMORY,
    CHROMA,
    PGVECTOR,
    QDRANT,
}
