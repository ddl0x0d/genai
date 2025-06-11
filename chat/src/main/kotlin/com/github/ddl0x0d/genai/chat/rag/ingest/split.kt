package com.github.ddl0x0d.genai.chat.rag.ingest

import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.DocumentSplitter
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment

class CompositeDocumentSplitter(
    val delegates: List<DocumentSplitter>,
) : DocumentSplitter {

    override fun split(document: Document): List<TextSegment> =
        delegates.firstNotNullOfOrNull { it.split(document) } ?: emptyList()
}

class ConditionalDocumentSplitter(
    val predicate: (Document) -> Boolean,
    val delegate: DocumentSplitter,
) : DocumentSplitter {

    override fun split(document: Document): List<TextSegment>? =
        document.takeIf(predicate)?.let(delegate::split)
}

class MarkdownDocumentSplitter : DocumentSplitter {

    override fun split(document: Document): List<TextSegment> {
        val result = mutableListOf<TextSegment>()
        val stack = ArrayDeque<TextSegment>()
        document.text().lines().forEach { line ->
            val updated: TextSegment = if (line.startsWith("#")) {
                val (pounds, header) = line.split(" ", limit = 2)
                val level: Int = pounds.length
                var last: TextSegment? = stack.lastOrNull()
                last?.changeText { it.trimEnd() }?.let(result::add)
                while (last != null && (last.level ?: Int.MAX_VALUE) >= level) {
                    stack.removeLast()
                    last = stack.lastOrNull()
                }
                val metadata: Metadata = document.metadata().copy().apply {
                    put("level", level.toString())
                    stack.forEachIndexed { index, segment ->
                        val key = "h${index + 1}"
                        put(key, segment.metadata().getString(key))
                    }
                    put("h$level", header)
                }
                TextSegment(line, metadata)
            } else { // not header line
                stack.removeLastOrNull()
                    ?.let { last -> last + line }
                    ?: TextSegment(line, document.metadata())
            }
            stack.addLast(updated)
        }
        stack.lastOrNull()?.changeText { it.trimEnd() }?.let(result::add)
        return result
    }

    private val TextSegment.level: Int?
        get() = metadata().getInteger("level")

    private operator fun TextSegment.plus(line: String): TextSegment =
        changeText { it + "\n" + line }

    private fun TextSegment.changeText(action: (String) -> String): TextSegment =
        TextSegment(action(text()), metadata())
}
