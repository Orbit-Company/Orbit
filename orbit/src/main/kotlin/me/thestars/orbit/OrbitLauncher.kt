package me.thestars.orbit

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import me.thestars.orbit.utils.common.Constants
import me.thestars.orbit.utils.HoconUtils.decodeFromString
import me.thestars.orbit.utils.HostnameUtils
import me.thestars.orbit.utils.common.checkConfigFile
import me.thestars.orbit.utils.installCoroutinesDebugProbes
import me.thestars.orbit.utils.serializable.OrbitConfig
import mu.KotlinLogging
import okio.IOException
import java.io.File
import kotlin.system.exitProcess

object OrbitLauncher {
    private val logger = KotlinLogging.logger { }

    @JvmStatic
    fun main(args: Array<String>) {
        installCoroutinesDebugProbes()

        val configFile = checkConfigFile()
        val config = readConfigFile<OrbitConfig>(configFile)
        val hostname = HostnameUtils.getHostname()
        val clusterId = if (config.discord.getClusterIdFromHostname) {
            try {
                hostname.split("-")[1].toInt()
            } catch (_: IndexOutOfBoundsException) {
                logger.error { "Invalid hostname ($hostname)! The hostname must contain '-' followed by a numeric ID." }
                exitProcess(1)
            } catch (_: NumberFormatException) {
                logger.error { "Invalid ID in hostname ($hostname)! The value after '-' must be a number." }
                exitProcess(1)
            }
        } else config.discord.replicaId

        val currentCluster = config.discord.clusters.find { it.id == clusterId }
            ?: run {
                logger.error { "Cluster $hostname ($clusterId) not found in config file." }
                exitProcess(1)
            }

        logger.info { "Starting Orbit on Cluster ${currentCluster.id} (${currentCluster.name})" }

        runBlocking {
            OrbitInstance(config, currentCluster).start()
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> readConfigFile(file: File): T {
        try {
            val json = file.readText()
            return Constants.HOCON.decodeFromString<T>(json)
        } catch (e: IOException) {
            e.printStackTrace()
            exitProcess(1)
        }
    }
}