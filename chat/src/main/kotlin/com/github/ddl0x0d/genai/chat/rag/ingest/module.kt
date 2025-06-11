package com.github.ddl0x0d.genai.chat.rag.ingest

import com.github.ddl0x0d.genai.chat.Logger
import com.github.ddl0x0d.genai.chat.model.GeminiConfig
import com.github.ddl0x0d.genai.chat.model.OllamaConfig
import com.github.ddl0x0d.genai.chat.model.OllamaConnection
import com.github.ddl0x0d.genai.chat.model.OpenAiConfig
import com.github.ddl0x0d.genai.chat.rag.EmbeddingModelType
import com.github.ddl0x0d.genai.chat.rag.RagConfig
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.DocumentParser
import dev.langchain4j.data.document.DocumentSplitter
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter
import dev.langchain4j.model.embedding.DimensionAwareEmbeddingModel
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.embedding.onnx.bgesmallen.BgeSmallEnEmbeddingModel
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel
import dev.langchain4j.model.ollama.OllamaEmbeddingModel
import dev.langchain4j.model.openai.OpenAiEmbeddingModel
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.module.dsl.singleOf
import org.koin.core.parameter.parametersOf
import org.koin.dsl.bind
import org.koin.dsl.module

private val klogger = KotlinLogging.logger {}

val ragEmbeddingModule = module {
    single<DimensionAwareEmbeddingModel> {
        val config: RagConfig = get()
        when (config.embedding.model) {
            EmbeddingModelType.BGE_SMALL_EN -> BgeSmallEnEmbeddingModel()
            EmbeddingModelType.GEMINI -> {
                val config: GeminiConfig = get()
                GoogleAiEmbeddingModel.builder()
                    .modelName(config.embeddingModel)
                    .apiKey(config.apiKey)
                    .build()
            }
            EmbeddingModelType.OLLAMA -> {
                val config: OllamaConfig = get()
                val connection: OllamaConnection = get()
                OllamaEmbeddingModel.builder()
                    .baseUrl(connection.baseUrl)
                    .modelName(config.embeddingModel)
                    .build()
            }
            EmbeddingModelType.OPENAI -> {
                val config: OpenAiConfig = get()
                OpenAiEmbeddingModel.builder()
                    .modelName(config.embeddingModel)
                    .apply { config.baseUrl?.let(::baseUrl) }
                    .apiKey(config.apiKey)
                    .build()
            }
        }
    }.bind(EmbeddingModel::class)
}

val ragIngestionModule = module {
    single<DocumentParser> {
        ApacheTikaDocumentParser()
    }
    singleOf(::Parser)
    single<IngestionListener> {
        val config: RagConfig = get()
        LoggingIngestionListener(
            logger = get<Logger> { parametersOf(klogger) },
            logDuration = config.ingestion.logDuration,
        )
    }
    single<DocumentSplitter> {
        val config: RagConfig = get()
        CompositeDocumentSplitter(
            delegates = listOf(
                ConditionalDocumentSplitter(
                    predicate = { it.metadata().getString(Document.FILE_NAME)?.endsWith(".md") ?: false },
                    delegate = MarkdownDocumentSplitter()
                ),
                ConditionalDocumentSplitter(
                    predicate = { true },
                    delegate = DocumentByParagraphSplitter(config.ingestion.maxSegmentSize, 0)
                ),
            )
        )
    }
    singleOf(::Ingestor)
}
