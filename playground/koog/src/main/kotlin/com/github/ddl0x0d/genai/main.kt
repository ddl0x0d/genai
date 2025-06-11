package com.github.ddl0x0d.genai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.OllamaModels
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.BindMode
import org.testcontainers.ollama.OllamaContainer

private val logger = KotlinLogging.logger {}

fun main() {
    val home: String = System.getProperty("user.home")
    OllamaContainer("ollama/ollama:0.9.2")
        .withLogConsumer { logger.debug { it.utf8StringWithoutLineEnding } }
        .withFileSystemBind("${home}/.ollama", "/root/.ollama", BindMode.READ_WRITE)
        .apply { start() }
        .use { ollama ->
            val agent = AIAgent(
                executor = simpleOllamaAIExecutor(baseUrl = ollama.endpoint),
                llmModel = OllamaModels.Meta.LLAMA_3_2,
            )
            runBlocking {
                agent.runAndGetResult("Hi, Llama!")?.let { response ->
                    println(response)
                }
            }
        }
}
