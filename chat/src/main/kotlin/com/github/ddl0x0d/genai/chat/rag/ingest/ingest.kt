package com.github.ddl0x0d.genai.chat.rag.ingest

import com.github.ddl0x0d.genai.chat.rag.RagConfig
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.DocumentSplitter
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.embedding.EmbeddingModel
import dev.langchain4j.model.output.Response
import dev.langchain4j.model.output.TokenUsage
import dev.langchain4j.store.embedding.EmbeddingStore
import dev.langchain4j.store.embedding.IngestionResult
import kotlin.time.TimedValue
import kotlin.time.measureTimedValue

class Ingestor(
    private val config: RagConfig,
    private val listener: IngestionListener,
    private val parser: Parser,
    private val splitter: DocumentSplitter,
    private val model: EmbeddingModel,
    private val store: EmbeddingStore<TextSegment>,
) {

    fun ingest(document: Any) {
        runCatching {
            measureTimedValue {
                val documents: TimedValue<List<Document>> = measureTimedValue { parser.parse(document) }
                listener.onLoaded(documents)
                ingest(documents.value)
            }
        }.onSuccess { result ->
            listener.onCompleted(result)
        }.onFailure { e ->
            listener.onError(e)
        }
    }

    private fun ingest(documents: List<Document>): IngestionResult {
        val totalTokenUsage = TokenUsage()
        val segments: TimedValue<List<TextSegment>> = measureTimedValue { splitter.splitAll(documents) }
        listener.onSplit(segments)

        val chunks: TimedValue<List<List<TextSegment>>> = measureTimedValue {
            segments.value.chunked(config.ingestion.chunkSize)
        }
        listener.onChunked(chunks)

        chunks.value.forEachIndexed { index, chunk ->
            val response: TimedValue<Response<List<Embedding>>> = measureTimedValue { model.embedAll(chunk) }
            listener.onEmbedded(index, chunks.value.size, response)
            store.addAll(response.value.content(), chunk)
            totalTokenUsage.add(response.value.tokenUsage())
        }
        return IngestionResult(totalTokenUsage)
    }
}
