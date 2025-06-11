package com.github.ddl0x0d.genai.chat.rag.ingest

import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.DocumentParser
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
import dev.langchain4j.data.document.loader.UrlDocumentLoader
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

class Parser(private val parser: DocumentParser) {

    fun parse(document: Any): List<Document> =
        when (document) {
            is String if (Path(document).exists()) -> parse(Path(document))
            is String if (URI(document).isAbsolute) -> parse(URI(document))
            is Path -> parse(document)
            is URI -> parse(document)
            else -> error("Couldn't parse document \"$document\".")
        }

    private fun parse(uri: URI): List<Document> {
        return listOf(UrlDocumentLoader.load(uri.toString(), parser))
    }

    private fun parse(path: Path): List<Document> {
        require(path.exists()) { "Path \"$path\" does not exist" }
        return if (path.isDirectory()) {
            FileSystemDocumentLoader.loadDocuments(path, parser)
        } else {
            listOf(FileSystemDocumentLoader.loadDocument(path, parser))
        }
    }
}
