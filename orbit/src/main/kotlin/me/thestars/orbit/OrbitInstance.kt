package me.thestars.orbit

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import me.thestars.orbit.database.DatabaseService
import me.thestars.orbit.database.PostgresConfig
import me.thestars.orbit.interactions.CommandManager
import me.thestars.orbit.interactions.components.ComponentManager
import me.thestars.orbit.listeners.InteractionsListener
import me.thestars.orbit.utils.TasksUtils
import me.thestars.orbit.utils.serializable.OrbitConfig
import me.thestars.orbit.utils.threads.ThreadPoolManager
import me.thestars.orbit.utils.threads.ThreadUtils
import mu.KotlinLogging
import net.dv8tion.jda.api.JDAInfo
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.requests.RestConfig
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import kotlin.concurrent.thread

class OrbitInstance(
    val config: OrbitConfig,
    val currentCluster: OrbitConfig.DiscordSettings.Cluster
) {
    lateinit var shardManager: ShardManager
    lateinit var interactionManager: ComponentManager
    private val activeJobs = ThreadUtils.activeJobs
    private val coroutineExecutor = ThreadUtils.createThreadPool("CoroutineExecutor [%d]")

    val database: DatabaseService = DatabaseService()

    val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    val restVersion = JDAInfo.DISCORD_REST_VERSION
    val baseUrl = config.discord.baseUrl
    val logger = KotlinLogging.logger { }
    val threadPoolManager = ThreadPoolManager()
    val coroutineDispatcher = coroutineExecutor.asCoroutineDispatcher()
    val timeZone = TimeZone.currentSystemDefault()
    val taskScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val configDb = PostgresConfig(
        url = config.database.url,
        username = config.database.username,
        password = config.database.password
    )

    val commandHandler: CommandManager by lazy { CommandManager(this) }

    val http: HttpClient by lazy {
        HttpClient(CIO) {
            install(HttpTimeout) { requestTimeoutMillis = 60_000 }
            install(ContentNegotiation) { json() }
        }
    }

    suspend fun start() {
        interactionManager = ComponentManager(this)

        database.connect(configDb)

        shardManager = DefaultShardManagerBuilder.create(
            GatewayIntent.GUILD_MEMBERS,
            GatewayIntent.MESSAGE_CONTENT,
            GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.SCHEDULED_EVENTS,
            GatewayIntent.GUILD_EXPRESSIONS,
            GatewayIntent.DIRECT_MESSAGES,
            GatewayIntent.GUILD_VOICE_STATES
        ).apply {
            if (baseUrl != null) {
                logger.info { "Using Discord base URL: $baseUrl" }

                setRestConfig(RestConfig().setBaseUrl("${baseUrl.removeSuffix("/")}/api/v$restVersion/"))
            }
        }
            .addEventListeners(
                InteractionsListener(this)
            )
            .setAutoReconnect(true)
            .setStatus(OnlineStatus.ONLINE)
            .setActivity(Activity.playing("Starting..."))
            .setShardsTotal(config.discord.totalShards)
            .setShards(currentCluster.minShard, currentCluster.maxShard)
            .setMemberCachePolicy(MemberCachePolicy.ALL)
            .setChunkingFilter(ChunkingFilter.NONE)
            .disableCache(CacheFlag.entries)
            .enableCache(CacheFlag.EMOJI, CacheFlag.STICKER, CacheFlag.MEMBER_OVERRIDES)
            .setToken(config.discord.token)
            .setEnableShutdownHook(false)
            .build()

        if (currentCluster.isMasterCluster) {
            TasksUtils.launchTasks(this)
        }

        this.commandHandler.handle()

        Runtime.getRuntime().addShutdownHook(thread(false) {
            try {
                logger.info { "Orbit is shutting down..." }
                shardManager.shards.forEach { shard ->
                    shard.removeEventListener(*shard.registeredListeners.toTypedArray())
                    logger.info { "Shutting down shard #${shard.shardInfo.shardId}..." }
                    shard.shutdown()
                }

                activeJobs.forEach {
                    logger.info { "Cancelling job $it" }
                    it.cancel()
                }

                http.close()
                database.close()
                coroutineExecutor.shutdown()
                threadPoolManager.shutdown()
                taskScope.cancel()
            } catch (e: Exception) {
                logger.error(e) { "Error during shutdown process" }
            }
        })
    }
}
