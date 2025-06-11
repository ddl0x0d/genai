@file:Suppress("MaxLineLength")

package com.github.ddl0x0d.genai.chat.cli

import com.github.ajalt.clikt.testing.test
import com.github.ddl0x0d.genai.chat.Config
import com.github.ddl0x0d.genai.chat.model.ChatConfig
import com.github.ddl0x0d.genai.chat.model.ChatModelType
import com.github.ddl0x0d.genai.chat.rag.EmbeddingModelType
import com.github.ddl0x0d.genai.chat.rag.EmbeddingStoreType
import com.github.ddl0x0d.genai.chat.rag.RagConfig
import com.github.ddl0x0d.genai.chat.test.OllamaMock
import com.github.ddl0x0d.genai.chat.test.OpenAiMock
import com.github.ddl0x0d.genai.chat.test.shouldComplete
import com.github.ddl0x0d.genai.chat.test.shouldFail
import com.github.ddl0x0d.genai.chat.test.shouldSucceed
import com.github.ddl0x0d.genai.chat.test.test
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.common.Slf4jNotifier
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.datatest.withData
import io.kotest.engine.spec.tempdir
import io.kotest.engine.spec.tempfile
import io.kotest.extensions.wiremock.ListenerMode.PER_TEST
import io.kotest.extensions.wiremock.WireMockListener
import io.kotest.matchers.ints.shouldBeZero
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import io.kotest.matchers.string.shouldStartWith
import org.koin.core.context.stopKoin

private val preamble = """
        Start chatting with AI Assistant.
        You can use the following commands:

        - /help - print this help message
        - /ingest <path> - ingest document(s)
        - /exit - finish conversation

        User> 

        """.trimIndent()

class MainTest : ExpectSpec({

    val server = WireMockServer(
        options()
            .dynamicPort()
            .notifier(Slf4jNotifier(true))
    )
    listener(WireMockListener(server, PER_TEST))
    val ollama = OllamaMock(server)
    val openai = OpenAiMock(server)

    lateinit var main: Main

    beforeTest {
        main = Main()
    }

    afterTest {
        stopKoin()
    }

    context("main") {
        context("--config") {
            expect("file does not exist") {
                main.test("-c wrong").shouldFail {
                    """
                    Usage: chat [<options>] <command> [<args>]...

                    Error: invalid value for -c: file "wrong" does not exist.

                    """.trimIndent()
                }
            }
            expect("file is a directory") {
                val dir = tempdir()
                main.test("-c $dir").shouldFail {
                    """
                    Usage: chat [<options>] <command> [<args>]...

                    Error: invalid value for -c: file "$dir" is a directory.

                    """.trimIndent()
                }
            }
            expect("file is not YAML") {
                val file = tempfile()
                main.test("-c $file").shouldFail {
                    """
                    Usage: chat [<options>] <command> [<args>]...

                    Error: invalid value for --config: file "${file.name}" is not YAML.

                    """.trimIndent()
                }
            }
            expect("config source is empty") {
                val config = tempfile(suffix = ".yaml")
                main.test("-c $config").shouldFail {
                    """
                    Usage: chat [<options>] <command> [<args>]...

                    Error: incorrect format in file ${config.name}: Error loading config because:

                        Config source $config is empty

                    """.trimIndent()
                }
            }
            expect("incorrect format") {
                val config = tempfile(suffix = ".yaml")
                config.writeText(
                    """
                    chat:
                      model: foo

                    rag:
                      embedding:
                        model: bar
                    """.trimIndent()
                )
                main.test("-c $config").shouldFail {
                    """
                    Usage: chat [<options>] <command> [<args>]...

                    Error: incorrect format in file ${config.name}: Error loading config because:

                        - Could not instantiate '${Config::class.qualifiedName}' because:

                            - 'chat': - Could not instantiate '${ChatConfig::class.qualifiedName}' because:

                                - 'model': Required a value for the Enum type ${ChatModelType::class.qualifiedName} but given value was foo ($config:1:9)

                            - 'rag': - Could not instantiate '${RagConfig::class.qualifiedName}' because:

                                - 'embedding': - Could not instantiate '${RagConfig.Embedding::class.qualifiedName}' because:

                                    - 'model': Required a value for the Enum type ${EmbeddingModelType::class.qualifiedName} but given value was bar ($config:5:11)

                    """.trimIndent()
                }
            }
            expect("model validation") {
                val config = tempfile(suffix = ".yaml")
                config.writeText(
                    """
                    chat:
                      memory:
                        type: tokens
                      model: ollama
                    """.trimIndent()
                )
                main.test("-c $config").shouldFail {
                    """
                    Configuration error: 'chat.memory.type: tokens' requires 'chat.model: openai'.

                    """.trimIndent()
                }
            }
            expect("ollama validation") {
                val config = tempfile(suffix = ".yaml")
                config.writeText(
                    """
                    chat:
                      model: ollama
                    model:
                      ollama:
                        base-url:
                        image-tag:
                    """.trimIndent()
                )
                main.test("-c $config").shouldFail {
                    """
                    Configuration error: either 'model.ollama.base-url' or 'model.ollama.image-tag' must be specified.

                    """.trimIndent()
                }
            }
        }
        expect("--help") {
            main.test("--help").shouldSucceed {
                """
                Usage: chat [<options>] <command> [<args>]...

                Options:
                  -c, --config=<path>  Specify configuration file
                  -h, --help           Show this message and exit

                Commands:
                  chat    Start chatting with AI Assistant
                  config  Print default config to stdout
                  ingest  Ingest document(s) to RAG store

                """.trimIndent()
            }
        }
        expect("/exit") {
            main.test { "/exit" }.shouldSucceed { preamble }
        }
        context("assistant") {
            expect("ollama") {
                val config = tempfile(suffix = ".yaml")
                config.writeText(
                    // language=yaml
                    """
                    chat:
                      model: ollama
                      memory:
                        type: messages
                      output:
                        tokens: true

                    model:
                      ollama:
                        base-url: ${server.baseUrl()}

                    rag:
                      embedding:
                        model: ollama
                    """.trimIndent()
                )
                ollama.run {
                    onEmbed().returnEmbeddings(0.1, 0.2, 0.3)
                    onChat().stream("Hi! How can I help you?")
                }
                main.test("-c $config") {
                    """
                    Hello!
                    /exit
                    """.trimIndent()
                }.shouldSucceed {
                    preamble + """
                    AI> Hi! How can I help you? (T=0/23)

                    User> 

                    """.trimIndent()
                }
                ollama.run {
                    verifyEmbedded("Hello!", "nomic-embed-text:latest")
                    verifyStreamed("Hello!", "llama3.2:1b")
                }
            }
            expect("openai") {
                val config = tempfile(suffix = ".yaml")
                config.writeText(
                    // language=yaml
                    """
                    chat:
                      model: openai
                      memory:
                        type: tokens
                      output:
                        tokens: false

                    model:
                      openai:
                        base-url: ${server.baseUrl()}
                        api-key: ${openai.token}

                    rag:
                      embedding:
                        model: openai
                    """.trimIndent()
                )
                openai.run {
                    onEmbed().returnEmbeddings(0.1, 0.2, 0.3)
                    onChat().stream("Hi! How can I help you?")
                }
                main.test("-c $config") {
                    """
                    Hello!
                    /exit
                    """.trimIndent()
                }.shouldSucceed {
                    preamble + """
                    AI> Hi! How can I help you?

                    User> 

                    """.trimIndent()
                }
                openai.run {
                    verifyEmbedded("Hello!", "text-embedding-3-small")
                    verifyStreamed("Hello!", "gpt-4.1-nano")
                }
            }
        }
    }

    context("chat") {
        expect("--help") {
            main.test("chat --help").shouldSucceed {
                """
                Usage: chat chat [<options>]

                  Start chatting with AI Assistant

                Options:
                  -h, --help  Show this message and exit

                """.trimIndent()
            }
        }
        expect("/help") {
            main.test("chat") {
                """
                /help
                /exit
                """.trimIndent()
            }.shouldSucceed {
                preamble + preamble
            }
        }
        context("/ingest") {
            expect("<path> does not exist") {
                main.test("chat") {
                    """
                    /ingest wrong
                    /exit
                    """.trimIndent()
                }.shouldComplete(
                    expectedStatusCode = 0,
                    expectedStdout = {
                        preamble + """

                        User> 

                        """.trimIndent()
                    },
                    expectedStderr = {
                        """
                        Ingestion failed: Couldn't parse document "wrong".

                        """.trimIndent()
                    },
                )
            }
            context("success") {
                withData(EmbeddingStoreType.entries) { type ->
                    val config = tempfile(suffix = ".yaml")
                    config.writeText(
                        """
                        rag:
                          store:
                            type: $type
                        """.trimIndent()
                    )
                    val file = tempfile(suffix = ".md")
                    file.writeText(
                        """
                        # Test file

                        Some test text
                        """.trimIndent()
                    )
                    main.test("-c $config chat") {
                        """
                        /ingest $file
                        /exit
                        """.trimIndent()
                    }.run {
                        assertSoftly {
                            statusCode.shouldBeZero()
                            stdout.run {
                                shouldStartWith(preamble)
                                shouldContain("Loaded 1 documents")
                                shouldContain("Split documents into 1 segments")
                                shouldContain("Chunked segments into 1 chunks")
                                shouldContain("Embedded chunk 1 out of 1")
                                shouldContain("Ingestion completed")
                            }
                            stderr.shouldBeEmpty()
                        }
                    }
                }
            }
        }
        expect("/exit") {
            main.test("chat") { "/exit" }.shouldSucceed { preamble }
        }
    }

    context("config") {
        expect("--help") {
            main.test("config --help").shouldSucceed {
                """
                Usage: chat config [<options>]

                  Print default config to stdout

                Options:
                  -h, --help  Show this message and exit

                """.trimIndent()
            }
        }
        expect("output") {
            main.test("config").run {
                assertSoftly {
                    statusCode.shouldBeZero()
                    stdout.shouldNotBeEmpty()
                    stderr.shouldBeEmpty()
                }
            }
        }
    }

    context("ingest") {
        expect("--help") {
            main.test("ingest --help").shouldSucceed {
                """
                Usage: chat ingest [<options>]

                  Ingest document(s) to RAG store

                Options:
                  -f, --file=<path>  Specify file to ingest
                  -u, --uri=<value>  Specify URI to ingest
                  -h, --help         Show this message and exit

                """.trimIndent()
            }
        }
        expect("--file does not exist") {
            main.test("ingest --file wrong").shouldFail {
                """
                Usage: chat ingest [<options>]

                Error: invalid value for --file: path "wrong" does not exist.

                """.trimIndent()
            }
        }
        expect("--uri is not absolute") {
            main.test("ingest --uri /wrong").shouldFail {
                """
                Usage: chat ingest [<options>]

                Error: invalid value for --uri: URI "/wrong" is not absolute.

                """.trimIndent()
            }
        }
        context("success") {
            withData(EmbeddingStoreType.entries) { type ->
                val config = tempfile(suffix = ".yaml")
                config.writeText(
                    """
                    rag:
                      store:
                        type: $type
                    """.trimIndent()
                )
                val file = tempfile(suffix = ".md")
                file.writeText(
                    """
                    # Test file

                    Some test text
                    """.trimIndent()
                )
                main.test("-c $config ingest -f $file").shouldSucceed { "" }
            }
        }
    }
})
