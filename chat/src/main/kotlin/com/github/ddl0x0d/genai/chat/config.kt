package com.github.ddl0x0d.genai.chat

import com.github.ddl0x0d.genai.chat.model.ChatConfig
import com.github.ddl0x0d.genai.chat.rag.RagConfig
import com.sksamuel.hoplite.ConfigBinder
import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.ExperimentalHoplite
import com.sksamuel.hoplite.addPathSource
import com.sksamuel.hoplite.addResourceSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import org.koin.core.parameter.ParametersHolder
import org.koin.dsl.module
import java.nio.file.Path

private val kotlinLogger = KotlinLogging.logger {}

@OptIn(ExperimentalHoplite::class)
val configModule = module {
    single<ConfigLoader> { params: ParametersHolder ->
        val configPath: Path? = params.getOrNull()
        ConfigLoader {
            configPath?.let(::addPathSource)
            addResourceSource("/default.yaml")
            withResolveTypesCaseInsensitive()
            withReportPrintFn { msg: String -> kotlinLogger.debug { msg } }
        }
    }
    single<ConfigBinder> { get<ConfigLoader>().configBinder() }
}

@Serializable
data class Config(
    val chat: ChatConfig,
    val rag: RagConfig,
)
