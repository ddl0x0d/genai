package com.github.ddl0x0d.genai.chat.model

import com.sksamuel.hoplite.ConfigBinder
import kotlinx.serialization.Serializable
import org.koin.dsl.module

private const val PREFIX = "model.gemini"

val geminiModule = module {
    single<GeminiConfig> { get<ConfigBinder>().bindOrThrow(PREFIX) }
}

@Serializable
data class GeminiConfig(
    val apiKey: String,
    val chatModel: String,
    val embeddingModel: String,
)
