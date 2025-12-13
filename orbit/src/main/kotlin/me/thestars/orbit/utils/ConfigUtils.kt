package me.thestars.orbit.utils

import java.io.File
import kotlin.system.exitProcess

fun checkConfigFile(): File {
    val path = System.getenv("CONF")
        ?: System.getProperty("conf")
        ?: "./orbit.conf"

    val configFile = File(path)

    if (!configFile.exists()) {
        println("O arquivo 'orbit.conf' não foi encontrado em: ${configFile.absolutePath}")
        exitProcess(1)
    }

    return configFile
}
