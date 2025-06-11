package com.github.ddl0x0d.genai

import org.jline.terminal.Terminal
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.ollama.api.OllamaApi
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption

@ShellComponent
@Suppress("unused")
class PromptCommands(
    private val api: OllamaApi,
    private val chat: ChatClient,
    private val terminal: Terminal,
) {

    @ShellMethod(key = ["list", "l"], value = "Show list of supported models.")
    fun listModels() {
        terminal.writer().println("Supported models:")
        api.listModels().models().forEach { model ->
            terminal.writer().println("- ${model.name}")
        }
    }

    @ShellMethod(key = ["prompt", "p"], value = "Send a prompt to model.")
    fun prompt(@ShellOption(arity = Int.MAX_VALUE) vararg words: String) {
        val prompt: String = words.joinToString(" ")
        chat.prompt(prompt).stream().content()
            .doOnNext { terminal.writer().print(it) }
            .blockLast()
        terminal.writer().println()
    }
}
