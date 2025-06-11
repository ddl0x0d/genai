package com.github.ddl0x0d.genai.chat.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.InvalidFileFormat
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.check
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import com.github.ddl0x0d.genai.chat.startApplication
import com.sksamuel.hoplite.ConfigException
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name

@Suppress("EmptyDefaultConstructor")
class Main() : CliktCommand("chat") {

    val configPath: Path? by option("--config", "-c", help = "Specify configuration file")
        .path(mustExist = true, canBeDir = false, mustBeReadable = true)
        .check(lazyMessage = { "file \"${it.name}\" is not YAML." }) {
            it.extension in setOf("yml", "yaml")
        }

    init {
        subcommands(Chat(), Config(), Ingest())
    }

    override val invokeWithoutSubcommand: Boolean = true

    override fun run() {
        try {
            startApplication(configPath, currentContext)
        } catch (@Suppress("SwallowedException") e: ConfigException) {
            throw InvalidFileFormat(configPath!!.name, e.message!!)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            throw CliktError(e.message, e)
        }
        if (currentContext.invokedSubcommand == null) {
            registeredSubcommands().first { it is Chat }.run()
        }
    }
}
