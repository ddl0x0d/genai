package com.github.ddl0x0d.genai.chat.test

interface ModelMock {

    fun onChat(): Chat
    fun verifyStreamed(input: String, model: String)
    fun onEmbed(): Embed
    fun verifyEmbedded(input: String, model: String)

    interface Chat {
        fun stream(content: String)
    }

    interface Embed {
        fun returnEmbeddings(vararg embeddings: Double)
    }
}
