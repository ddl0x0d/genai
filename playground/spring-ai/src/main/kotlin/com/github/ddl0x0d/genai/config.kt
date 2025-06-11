package com.github.ddl0x0d.genai

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import java.nio.charset.StandardCharsets.UTF_8

@Configuration(proxyBeanMethods = false)
class ChatClientConfiguration {

    @Bean
    fun chatClient(
        chatBuilder: ChatClient.Builder,
//        toolCallbackProvider: ToolCallbackProvider,
        @Value($$"${classpath:prompt.txt}") resource: Resource,
    ): ChatClient = chatBuilder
        .defaultAdvisors(
            PromptChatMemoryAdvisor.builder(
                MessageWindowChatMemory.builder().build()
            ).build(),
            SimpleLoggerAdvisor(),
        )
        .defaultSystem(resource.getContentAsString(UTF_8))
//        .defaultToolCallbacks(toolCallbackProvider)
        .build()
}
