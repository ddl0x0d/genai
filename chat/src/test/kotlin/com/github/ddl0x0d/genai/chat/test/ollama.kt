package com.github.ddl0x0d.genai.chat.test

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.marcinziolo.kotlin.wiremock.BuildingStep
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returns
import com.marcinziolo.kotlin.wiremock.verify

class OllamaMock(private val server: WireMockServer) : ModelMock {

    /**
     * @see dev.langchain4j.model.ollama.OllamaStreamingChatModel.doChat
     */
    override fun onChat(): Chat = Chat(server.post { urlPath equalTo "/api/chat" })

    /**
     * @see dev.langchain4j.model.ollama.OllamaStreamingChatModel.doChat
     */
    override fun verifyStreamed(input: String, model: String) {
        server.verify {
            exactly = 1
            method = RequestMethod.POST
            urlPath equalTo "/api/chat"
            // language=json
            body equalTo """
                {
                  "model": "$model",
                  "messages": [
                    {
                      "role": "user",
                      "content": "$input"
                    }
                  ],
                  "options": {
                    "stop": []
                  },
                  "stream": true,
                  "tools": []
                }
                """.trimIndent()
        }
    }

    /**
     * @see dev.langchain4j.model.ollama.OllamaEmbeddingModel.embedAll
     */
    override fun onEmbed(): Embed = Embed(server.post { urlPath equalTo "/api/embed" })

    /**
     * @see dev.langchain4j.model.ollama.OllamaEmbeddingModel.embedAll
     */
    override fun verifyEmbedded(input: String, model: String) {
        server.verify {
            exactly = 1
            method = RequestMethod.POST
            urlPath equalTo "/api/embed"
            // language=json
            body equalTo """
                {
                  "model" : "$model",
                  "input" : [ "$input" ]
                }
                """.trimIndent()
        }
    }

    /**
     * @see dev.langchain4j.model.ollama.OllamaStreamingChatModel.doChat
     */
    class Chat(private val step: BuildingStep) : ModelMock.Chat {

        override fun stream(content: String) {
            // language=json
            val bodyTemplate = """
                {
                  "message": {
                    "role": "assistant",
                    "content": "<CONTENT>"
                  },
                  "done_reason": "stop",
                  "done": true,
                  "prompt_eval_count": 0,
                  "eval_count": ${content.length}
                }
            """.trimIndent()
            step.returns {
                header = "Content-Type" to "application/json"
                body = bodyTemplate
                    .replace("\\s".toRegex(), "")
                    .replace("<CONTENT>", content)
            }
        }
    }

    /**
     * @see dev.langchain4j.model.ollama.OllamaEmbeddingModel.embedAll
     */
    class Embed(private val step: BuildingStep) : ModelMock.Embed {

        override fun returnEmbeddings(vararg embeddings: Double) {
            step.returns {
                header = "Content-Type" to "application/json"
                // language=json
                body = """
                       {
                         "embeddings": [[${embeddings.joinToString()}]]
                       }
                       """.trimIndent()
            }
        }
    }
}
