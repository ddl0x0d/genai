package com.github.ddl0x0d.genai.chat.model

import com.github.ddl0x0d.genai.chat.Initializer
import kotlinx.serialization.Serializable

@Serializable
data class ChatConfig(
    val debug: Boolean,
    val model: ChatModelType,
    val memory: Memory,
    val output: Output,
) {
    @Serializable
    data class Output(
        val tokens: Boolean,
    )
}

class ChatConfigValidator(private val config: ChatConfig) : Initializer {
    override fun init() {
        if (config.memory.type == Memory.Type.TOKENS) {
            require(config.model == ChatModelType.OPENAI) {
                "Configuration error: '$CHAT_PREFIX.memory.type: tokens' requires '$CHAT_PREFIX.model: openai'."
            }
        }
    }
}

enum class ChatModelType {
    GEMINI,
    OLLAMA,
    OPENAI,
}

@Serializable
data class Memory(
    val size: Int,
    val type: Type,
) {
    enum class Type {
        MESSAGES,
        TOKENS,
    }
}
