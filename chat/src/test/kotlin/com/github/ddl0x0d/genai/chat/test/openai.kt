package com.github.ddl0x0d.genai.chat.test

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.marcinziolo.kotlin.wiremock.BuildingStep
import com.marcinziolo.kotlin.wiremock.contains
import com.marcinziolo.kotlin.wiremock.equalTo
import com.marcinziolo.kotlin.wiremock.post
import com.marcinziolo.kotlin.wiremock.returns
import com.marcinziolo.kotlin.wiremock.verify
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class OpenAiMock(private val server: WireMockServer) : ModelMock {

    @OptIn(ExperimentalUuidApi::class)
    val token: String = "${Uuid.random()}"

    /**
     * @see dev.langchain4j.model.openai.OpenAiStreamingChatModel.doChat
     */
    override fun onChat(): Chat = Chat(
        server.post {
            urlPath equalTo "/chat/completions"
            headers contains "Authorization" equalTo "Bearer $token"
        }
    )

    /**
     * @see dev.langchain4j.model.openai.OpenAiStreamingChatModel.doChat
     */
    override fun verifyStreamed(input: String, model: String) {
        server.verify {
            exactly = 1
            method = RequestMethod.POST
            urlPath equalTo "/chat/completions"
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
                  "stream": true,
                  "stream_options" : {
                    "include_usage" : true
                  }
                }
                """.trimIndent()
        }
    }

    /**
     * @see dev.langchain4j.model.openai.OpenAiEmbeddingModel.embedTexts
     */
    override fun onEmbed(): Embed = Embed(
        server.post {
            urlPath equalTo "/embeddings"
            headers contains "Authorization" equalTo "Bearer $token"
        }
    )

    /**
     * @see dev.langchain4j.model.openai.OpenAiEmbeddingModel.embedTexts
     */
    override fun verifyEmbedded(input: String, model: String) {
        server.verify {
            exactly = 1
            method = RequestMethod.POST
            urlPath equalTo "/embeddings"
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
     * @see dev.langchain4j.model.openai.OpenAiStreamingChatModel.doChat
     */
    class Chat(private val step: BuildingStep) : ModelMock.Chat {

        override fun stream(content: String) {
            // language=json
            val bodyTemplate = """
                {
                  "choices": [
                    {
                      "delta": {
                        "role": "assistant",
                        "content": "<CONTENT>"
                      },
                      "finish_reason": "stop"
                    }
                  ]
                }
            """.trimIndent()
            step.returns {
                header = "Content-Type" to "text/event-stream"
                body = "data:${
                    bodyTemplate
                        .replace("\\s".toRegex(), "")
                        .replace("<CONTENT>", content)
                }\n\n"
            }
        }
    }

    /**
     * @see dev.langchain4j.model.openai.OpenAiEmbeddingModel.embedTexts
     */
    class Embed(private val step: BuildingStep) : ModelMock.Embed {

        override fun returnEmbeddings(vararg embeddings: Double) {
            step.returns {
                header = "Content-Type" to "application/json"
                // language=json
                body = """
                       {
                         "data": [
                            {
                              "embedding": [${embeddings.joinToString()}]
                            }
                         ]
                       }
                       """.trimIndent()
            }
        }
    }
}
