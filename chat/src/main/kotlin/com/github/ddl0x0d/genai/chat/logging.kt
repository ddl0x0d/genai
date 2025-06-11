package com.github.ddl0x0d.genai.chat

import com.github.ajalt.clikt.core.Context
import com.github.ddl0x0d.genai.chat.cli.Chat
import io.github.oshai.kotlinlogging.KLogger
import org.koin.core.parameter.ParametersHolder
import org.koin.dsl.module

fun loggingModule(context: Context) = module {
    factory<Logger> { params: ParametersHolder ->
        if (context.invokedSubcommand == null || context.invokedSubcommand is Chat) {
            CliktLogger(context)
        } else {
            val logger: KLogger = params.get()
            Slf4jLogger(logger)
        }
    }
}

interface Logger {
    fun info(message: String)
    fun error(message: String)
}

class Slf4jLogger(val logger: KLogger) : Logger {

    override fun info(message: String) {
        logger.info { message }
    }

    override fun error(message: String) {
        logger.error { message }
    }
}

class CliktLogger(val context: Context) : Logger {

    override fun info(message: String) {
        context.echoMessage(context, message, true, false)
    }

    override fun error(message: String) {
        context.echoMessage(context, message, true, true)
    }
}
