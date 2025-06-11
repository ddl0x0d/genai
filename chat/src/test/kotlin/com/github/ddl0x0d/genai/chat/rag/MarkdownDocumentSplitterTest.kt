package com.github.ddl0x0d.genai.chat.rag

import com.github.ddl0x0d.genai.chat.rag.ingest.MarkdownDocumentSplitter
import dev.langchain4j.data.document.DefaultDocument
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.DocumentSplitter
import dev.langchain4j.data.document.Metadata
import dev.langchain4j.data.segment.TextSegment
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.collections.shouldContainExactly

class MarkdownDocumentSplitterTest : ExpectSpec({

    val splitter: DocumentSplitter = MarkdownDocumentSplitter()

    val metadata: Metadata = Metadata.metadata(Document.FILE_NAME, "test.md")

    context("plain text") {
        expect("single line") {
            val document = DefaultDocument("plain text", metadata)

            val result: List<TextSegment> = splitter.split(document)

            result.shouldContainExactly(TextSegment("plain text", metadata))
        }
        expect("multi line") {
            val document = DefaultDocument(
                """
                This is
                a multi
                line text
                """.trimIndent(),
                metadata
            )

            val result: List<TextSegment> = splitter.split(document)

            result.shouldContainExactly(TextSegment(document.text(), metadata))
        }
    }
    context("markdown") {
        expect("single header") {
            val document = DefaultDocument(
                """
                # Header

                Section
                """.trimIndent(), metadata
            )

            val result: List<TextSegment> = splitter.split(document)

            result.shouldContainExactly(
                TextSegment(
                    document.text(),
                    metadata.copy()
                        .put("level", "1")
                        .put("h1", "Header")
                ),
            )
        }
        context("multiple headers") {
            expect("consecutive") {
                val document = DefaultDocument(
                    """
                    # Header 1

                    Section 1

                    ## Header 2

                    Section 2

                    ### Header 3

                    Section 3
                    """.trimIndent(), metadata
                )

                val result: List<TextSegment> = splitter.split(document)

                result.shouldContainExactly(
                    TextSegment(
                        """
                        # Header 1

                        Section 1
                        """.trimIndent(),
                        metadata.copy()
                            .put("level", "1")
                            .put("h1", "Header 1")
                    ),
                    TextSegment(
                        """
                        ## Header 2

                        Section 2
                        """.trimIndent(),
                        metadata.copy()
                            .put("level", "2")
                            .put("h1", "Header 1")
                            .put("h2", "Header 2")
                    ),
                    TextSegment(
                        """
                        ### Header 3

                        Section 3
                        """.trimIndent(),
                        metadata.copy()
                            .put("level", "3")
                            .put("h1", "Header 1")
                            .put("h2", "Header 2")
                            .put("h3", "Header 3")
                    ),
                )
            }
            expect("alternating") {
                val document = DefaultDocument(
                    """
                    # Header 1

                    Section 1

                    ## Header 2a

                    Section 2a

                    ### Header 3a

                    Section 3a

                    ## Header 2b

                    Section 2b

                    ### Header 3b

                    Section 3b
                    """.trimIndent(), metadata
                )

                val result: List<TextSegment> = splitter.split(document)

                result.shouldContainExactly(
                    TextSegment(
                        """
                        # Header 1

                        Section 1
                        """.trimIndent(),
                        metadata.copy()
                            .put("level", "1")
                            .put("h1", "Header 1")
                    ),
                    TextSegment(
                        """
                        ## Header 2a

                        Section 2a
                        """.trimIndent(),
                        metadata.copy()
                            .put("level", "2")
                            .put("h1", "Header 1")
                            .put("h2", "Header 2a")
                    ),
                    TextSegment(
                        """
                        ### Header 3a

                        Section 3a
                        """.trimIndent(),
                        metadata.copy()
                            .put("level", "3")
                            .put("h1", "Header 1")
                            .put("h2", "Header 2a")
                            .put("h3", "Header 3a")
                    ),
                    TextSegment(
                        """
                        ## Header 2b

                        Section 2b
                        """.trimIndent(),
                        metadata.copy()
                            .put("level", "2")
                            .put("h1", "Header 1")
                            .put("h2", "Header 2b")
                    ),
                    TextSegment(
                        """
                        ### Header 3b

                        Section 3b
                        """.trimIndent(),
                        metadata.copy()
                            .put("level", "3")
                            .put("h1", "Header 1")
                            .put("h2", "Header 2b")
                            .put("h3", "Header 3b")
                    ),
                )
            }
        }
        expect("YAML front matter") {
            val document = DefaultDocument(
                """
                ---
                foo: bar
                ---
                # Header

                Section
                """.trimIndent(), metadata
            )

            val result: List<TextSegment> = splitter.split(document)

            result.shouldContainExactly(
                TextSegment(
                    """
                    ---
                    foo: bar
                    ---
                    """.trimIndent(),
                    metadata.copy()
                ),
                TextSegment(
                    """
                    # Header

                    Section
                    """.trimIndent(),
                    metadata.copy()
                        .put("level", "1")
                        .put("h1", "Header")
                ),
            )
        }
    }
})
