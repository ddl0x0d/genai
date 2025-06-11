package com.github.ddl0x0d.genai.chat.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ddl0x0d.genai.chat.Assistant
import com.github.ddl0x0d.genai.chat.model.ChatConfig
import com.github.ddl0x0d.genai.chat.rag.ingest.Ingestor
import dev.langchain4j.model.chat.response.ChatResponse
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.CountDownLatch

class Chat : CliktCommand(), KoinComponent {

    private val assistant: Assistant by inject()
    private val ingestor: Ingestor by inject()
    private val config: ChatConfig by inject()

    override fun help(context: Context): String = "Start chatting with AI Assistant"

    override fun run() {
        help()
        do {
            echo("User> ", trailingNewline = false)
            val input = readLine() ?: error("Couldn't read user input")
            echo()
        } while (proceed(input))
    }

    private fun proceed(input: String): Boolean {
        when (input) {
            "/help" -> help()
            "/exit" -> return false
            else -> when {
                input.startsWith("/ingest ") -> ingest(input)
                else -> chat(input)
            }
        }
        return true
    }

    private fun help() {
        echo("Start chatting with AI Assistant.")
        echo("You can use the following commands:")
        echo()
        echo("- /help - print this help message")
        echo("- /ingest <path> - ingest document(s)")
        echo("- /exit - finish conversation")
        echo()
    }

    private fun ingest(message: String) {
        val input = message.substringAfter("/ingest ")
        ingestor.ingest(input)
        echo()
    }

    @Suppress("MemberNameEqualsClassName")
    private fun chat(message: String) {
        var promptEchoed = false
        val latch = CountDownLatch(1)
        assistant.chat(message)
            .onPartialResponse {
                if (!promptEchoed) {
                    echo("AI> ", trailingNewline = false)
                    promptEchoed = true
                }
                echo(it, trailingNewline = false)
            }
            .onCompleteResponse { response: ChatResponse ->
                if (config.output.tokens) {
                    val tokens = response.tokenUsage().run { "${inputTokenCount()}/${outputTokenCount()}" }
                    echo(" (T=$tokens)", trailingNewline = false)
                }
                echo()
                echo()
                latch.countDown()
            }
            .onError { e: Throwable ->
                echo()
                echo("Error: ${e.message}", err = true)
                echo()
                latch.countDown()
            }
            .start()
        latch.await()
    }
}

