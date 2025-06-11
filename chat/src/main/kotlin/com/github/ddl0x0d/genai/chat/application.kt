package com.github.ddl0x0d.genai.chat

import com.github.ajalt.clikt.core.Context
import com.github.ddl0x0d.genai.chat.model.loadModelModules
import com.github.ddl0x0d.genai.chat.rag.loadRagModules
import com.sksamuel.hoplite.ConfigLoader
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.parameter.parametersOf
import org.koin.logger.slf4jLogger
import java.nio.file.Path

fun startApplication(configPath: Path?, context: Context) {
    val application: KoinApplication = startKoin {
        slf4jLogger()
        modules(configModule)
        modules(loggingModule(context))
    }
    application.apply {
        val loader: ConfigLoader = koin.get { parametersOf(configPath) }
        val config: Config = loader.loadConfigOrThrow<Config>()
        loadModelModules(config)
        loadRagModules(config)
        modules(assistantModule)
        val initializers = koin.getAll<Initializer>()
        initializers.forEach { it.init() }
    }
}

fun interface Initializer {
    fun init()
}
