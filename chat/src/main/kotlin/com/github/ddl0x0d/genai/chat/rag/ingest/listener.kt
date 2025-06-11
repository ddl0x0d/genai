package com.github.ddl0x0d.genai.chat.rag.ingest

import com.github.ddl0x0d.genai.chat.Logger
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.embedding.Embedding
import dev.langchain4j.data.segment.TextSegment
import dev.langchain4j.model.output.Response
import dev.langchain4j.store.embedding.IngestionResult
import kotlin.time.Duration
import kotlin.time.TimedValue

interface IngestionListener {
    fun onLoaded(documents: TimedValue<List<Document>>) {}
    fun onSplit(segments: TimedValue<List<TextSegment>>) {}
    fun onChunked(chunks: TimedValue<List<List<TextSegment>>>) {}
    fun onEmbedded(chunkIndex: Int, totalChunks: Int, embeddings: TimedValue<Response<List<Embedding>>>) {}
    fun onCompleted(result: TimedValue<IngestionResult>)
    fun onError(e: Throwable)
}

class LoggingIngestionListener(
    private val logger: Logger,
    private val logDuration: Boolean,
) : IngestionListener {

    override fun onLoaded(documents: TimedValue<List<Document>>) {
        log("Loaded ${documents.value.size} documents", documents.duration)
    }

    override fun onSplit(segments: TimedValue<List<TextSegment>>) {
        log("Split documents into ${segments.value.size} segments", segments.duration)
    }

    override fun onChunked(chunks: TimedValue<List<List<TextSegment>>>) {
        log("Chunked segments into ${chunks.value.size} chunks", chunks.duration)
    }

    override fun onEmbedded(
        chunkIndex: Int,
        totalChunks: Int,
        embeddings: TimedValue<Response<List<Embedding>>>,
    ) {
        log("Embedded chunk ${chunkIndex + 1} out of $totalChunks", embeddings.duration)
    }

    override fun onCompleted(result: TimedValue<IngestionResult>) {
        log("Ingestion completed", result.duration)
        result.value.tokenUsage()?.let {
            val input: Int? = it.inputTokenCount()
            val output: Int? = it.outputTokenCount()
            if (input != null && output != null) {
                logger.info("Total token usage: $input/$output")
            }
        }
    }

    override fun onError(e: Throwable) {
        logger.error("Ingestion failed: ${e.message}")
    }

    private fun log(message: String, duration: Duration) {
        logger.info(buildString {
            append(message)
            if (logDuration) {
                append(" in $duration")
            }
        })
    }
}
