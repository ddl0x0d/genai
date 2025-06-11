package com.github.ddl0x0d.genai.chat.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ddl0x0d.genai.chat.rag.ingest.Ingestor
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.net.URI

class Ingest : CliktCommand(), KoinComponent {

    private val ingestor: Ingestor by inject()

    val files: List<File> by option("--file", "-f", help = "Specify file to ingest")
        .file(mustExist = true, mustBeReadable = true)
        .multiple()

    val uris: List<URI> by option("--uri", "-u", help = "Specify URI to ingest")
        .convert { URI.create(it) }
        .multiple()
        .check(lazyMessage = { "URI \"${it.first { !it.isAbsolute }}\" is not absolute." }) { uris ->
            uris.all { it.isAbsolute }
        }

    override fun help(context: Context): String = "Ingest document(s) to RAG store"

    override fun run() {
        files.map(File::toPath).forEach(ingestor::ingest)
        uris.forEach(ingestor::ingest)
    }
}
