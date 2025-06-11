package com.github.ddl0x0d.genai.chat.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context

class Config : CliktCommand() {

    override fun help(context: Context): String = "Print default config to stdout"

    override fun run() {
        Config::class.java.classLoader.getResourceAsStream("default.yaml")?.let { resource ->
            resource.bufferedReader().use {
                echo(it.readText())
            }
        } ?: run {
            echo("Couldn't load default configuration", err = true)
        }
    }
}
