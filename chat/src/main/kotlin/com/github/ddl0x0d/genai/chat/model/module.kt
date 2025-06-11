package com.github.ddl0x0d.genai.chat.model

import com.github.ddl0x0d.genai.chat.Config
import com.github.ddl0x0d.genai.chat.Initializer
import com.sksamuel.hoplite.ConfigBinder
import dev.langchain4j.memory.ChatMemory
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.memory.chat.TokenWindowChatMemory
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel
import dev.langchain4j.model.ollama.OllamaStreamingChatModel
import dev.langchain4j.model.openai.OpenAiStreamingChatModel
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator
import org.koin.core.KoinApplication
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

fun KoinApplication.loadModelModules(config: Config) {
    loadModelTypesModules(config)
    loadModelMemoryModules(config)
    modules(chatConfigModule)
    modules(chatModelModule)
}

private fun KoinApplication.loadModelTypesModules(config: Config) {
    val modelNames: Set<String> = setOf(config.chat.model.name, config.rag.embedding.model.name)
    if ("GEMINI" in modelNames) modules(geminiModule)
    if ("OLLAMA" in modelNames) modules(ollamaModule)
    if ("OPENAI" in modelNames) modules(openAiModule)
}

private fun KoinApplication.loadModelMemoryModules(config: Config) {
    config.chat.memory.let { (size, type) ->
        modules(module {
            single<ChatMemory> {
                when (type) {
                    Memory.Type.MESSAGES -> MessageWindowChatMemory.withMaxMessages(size)
                    Memory.Type.TOKENS -> {
                        val config: OpenAiConfig = get()
                        TokenWindowChatMemory.withMaxTokens(
                            size, OpenAiTokenCountEstimator(config.chatModel)
                        )
                    }
                }
            }
        })
    }
}

const val CHAT_PREFIX = "chat"

private val chatConfigModule = module {
    single<ChatConfig> { get<ConfigBinder>().bindOrThrow<ChatConfig>(CHAT_PREFIX) }
    singleOf(::ChatConfigValidator).bind(Initializer::class)
}

private val chatModelModule = module {
    single<StreamingChatModel> {
        val chat: ChatConfig = get()
        when (chat.model) {
            ChatModelType.GEMINI -> {
                val config: GeminiConfig = get()
                GoogleAiGeminiStreamingChatModel.builder()
                    .apiKey(config.apiKey)
                    .modelName(config.chatModel)
                    .build()
            }
            ChatModelType.OLLAMA -> {
                val config: OllamaConfig = get()
                val connection: OllamaConnection = get()
                OllamaStreamingChatModel.builder()
                    .baseUrl(connection.baseUrl)
                    .modelName(config.chatModel)
                    .logRequests(chat.debug)
                    .logResponses(chat.debug)
                    .build()
            }
            ChatModelType.OPENAI -> {
                val config: OpenAiConfig = get()
                OpenAiStreamingChatModel.builder()
                    .modelName(config.chatModel)
                    .apply { config.baseUrl?.let(::baseUrl) }
                    .apiKey(config.apiKey)
                    .logRequests(chat.debug)
                    .logResponses(chat.debug)
                    .build()
            }
        }
    }
}
