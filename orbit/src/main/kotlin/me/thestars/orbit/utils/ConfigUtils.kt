package me.thestars.orbit.utils

import java.io.File
import kotlin.system.exitProcess

fun checkConfigFile(): File {
    val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("orbit.conf")
        ?: run {
            println("O arquivo 'orbit.conf' não foi encontrado nos resources.")
            exitProcess(1)
        }

    val temp = File.createTempFile("orbit", ".conf")
    temp.outputStream().use { stream.copyTo(it) }
    return temp
}
