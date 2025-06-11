package com.github.ddl0x0d.genai.chat

import dev.langchain4j.memory.ChatMemory
import dev.langchain4j.model.chat.StreamingChatModel
import dev.langchain4j.rag.RetrievalAugmentor
import dev.langchain4j.service.AiServices
import dev.langchain4j.service.TokenStream
import org.koin.dsl.module

val assistantModule = module {
    single<Assistant> {
        AiServices.builder(Assistant::class.java)
            .streamingChatModel(get<StreamingChatModel>())
            .chatMemory(get<ChatMemory>())
            .retrievalAugmentor(get<RetrievalAugmentor>())
            .build()
    }
}

interface Assistant {
    fun chat(message: String): TokenStream
}
