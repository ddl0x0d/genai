package com.github.ddl0x0d.genai.chat.rag

import com.github.ddl0x0d.genai.chat.Config
import com.github.ddl0x0d.genai.chat.model.ChatConfig
import com.github.ddl0x0d.genai.chat.model.ChatModelType
import com.github.ddl0x0d.genai.chat.model.GeminiConfig
import com.github.ddl0x0d.genai.chat.model.OllamaConfig
import com.github.ddl0x0d.genai.chat.model.OllamaConnection
import com.github.ddl0x0d.genai.chat.model.OpenAiConfig
import com.github.ddl0x0d.genai.chat.rag.ingest.ragEmbeddingModule
import com.github.ddl0x0d.genai.chat.rag.ingest.ragIngestionModule
import com.github.ddl0x0d.genai.chat.rag.store.chromaModule
import com.github.ddl0x0d.genai.chat.rag.store.pgVectorModule
import com.github.ddl0x0d.genai.chat.rag.store.qdrantModule
import com.github.ddl0x0d.genai.chat.rag.store.ragStoreModule
import com.sksamuel.hoplite.ConfigBinder
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel
import dev.langchain4j.model.ollama.OllamaChatModel
import dev.langchain4j.model.openai.OpenAiChatModel
import dev.langchain4j.rag.DefaultRetrievalAugmentor
import dev.langchain4j.rag.RetrievalAugmentor
import dev.langchain4j.rag.content.retriever.ContentRetriever
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever
import dev.langchain4j.rag.query.router.DefaultQueryRouter
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter
import dev.langchain4j.rag.query.router.QueryRouter
import dev.langchain4j.store.embedding.EmbeddingStore
import org.koin.core.KoinApplication
import org.koin.dsl.module

fun KoinApplication.loadRagModules(config: Config) {
    loadStoreTypeModule(config)
    modules(
        ragConfigModule,
        ragEmbeddingModule,
        ragIngestionModule,
        ragRetrievalModule,
        ragStoreModule,
    )
    if (config.rag.router.enabled) {
        modules(ragRouterModule)
    }
}

private fun KoinApplication.loadStoreTypeModule(config: Config) {
    when (config.rag.store.type) {
        EmbeddingStoreType.CHROMA -> modules(chromaModule)
        EmbeddingStoreType.IN_MEMORY -> {}
        EmbeddingStoreType.PGVECTOR -> modules(pgVectorModule)
        EmbeddingStoreType.QDRANT -> modules(qdrantModule)
    }
}

private val ragConfigModule = module {
    single<RagConfig> { get<ConfigBinder>().bindOrThrow<RagConfig>("rag") }
}

private val ragRetrievalModule = module {
    single<ContentRetriever> {
        val config: RagConfig = get()
        EmbeddingStoreContentRetriever.builder()
            .maxResults(config.retrieval.maxResults)
            .minScore(config.retrieval.minScore)
            .embeddingStore(get<EmbeddingStore<TextSegment>>())
            .embeddingModel(get<EmbeddingModel>())
            .build()
    }
    single<RetrievalAugmentor> {
        val config: RagConfig = get()
        val retriever: ContentRetriever = get()
        val router: QueryRouter = if (config.router.enabled) {
            val chatModel: ChatModel = get()
            LanguageModelQueryRouter.builder()
                .chatModel(chatModel)
                .retrieverToDescription(mapOf(retriever to "knowledge base"))
                .build()
        } else {
            DefaultQueryRouter(retriever)
        }
        DefaultRetrievalAugmentor.builder()
            .queryRouter(router)
            .build()
    }
}

private val ragRouterModule = module {
    single<ChatModel> {
        val rag: RagConfig = get()
        val chat: ChatConfig = get()
        when (rag.router.model) {
            ChatModelType.GEMINI -> {
                val config: GeminiConfig = get()
                GoogleAiGeminiChatModel.builder()
                    .apiKey(config.apiKey)
                    .modelName(config.chatModel)
                    .build()
            }
            ChatModelType.OLLAMA -> {
                val config: OllamaConfig = get()
                val connection: OllamaConnection = get()
                OllamaChatModel.builder()
                    .baseUrl(connection.baseUrl)
                    .modelName(config.chatModel)
                    .logRequests(chat.debug)
                    .logResponses(chat.debug)
                    .build()
            }
            ChatModelType.OPENAI -> {
                val config: OpenAiConfig = get()
                OpenAiChatModel.builder()
                    .modelName(config.chatModel)
                    .apply { config.baseUrl?.let(::baseUrl) }
                    .apiKey(config.apiKey)
                    .logRequests(chat.debug)
                    .logResponses(chat.debug)
                    .build()
            }
        }
    }
}
