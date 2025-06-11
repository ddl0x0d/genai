package com.github.ddl0x0d.genai.chat.model

import com.sksamuel.hoplite.ConfigBinder
import dev.langchain4j.model.openai.OpenAiChatModelName
import dev.langchain4j.model.openai.OpenAiEmbeddingModelName
import kotlinx.serialization.Serializable
import org.koin.dsl.module

private const val PREFIX = "model.openai"

val openAiModule = module {
    single<OpenAiConfig> { get<ConfigBinder>().bindOrThrow(PREFIX) }
}

@Serializable
data class OpenAiConfig(
    val baseUrl: String? = null,
    val apiKey: String,
    val chatModel: OpenAiChatModelName,
    val embeddingModel: OpenAiEmbeddingModelName,
)
