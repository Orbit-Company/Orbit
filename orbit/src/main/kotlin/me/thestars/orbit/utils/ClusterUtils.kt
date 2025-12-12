package me.thestars.orbit.utils

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import dev.minn.jda.ktx.coroutines.await
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import me.thestars.orbit.OrbitInstance
import me.thestars.orbit.utils.serializable.OrbitConfig
import me.thestars.orbit.utils.serializable.cluster.ClusterInfo
import me.thestars.orbit.utils.serializable.cluster.CustomGuildInfo
import me.thestars.orbit.utils.serializable.cluster.CustomMemberResponse
import mu.KotlinLogging
import java.util.concurrent.TimeUnit

object ClusterUtils {
    private val logger = KotlinLogging.logger { }
    private val cachedRoles: Cache<String, List<String>> = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(
            10, TimeUnit.MINUTES
        ).build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    fun getClusterByShardId(instance: OrbitInstance, shardId: Int): OrbitConfig.DiscordSettings.Cluster {
        val shard = instance.config.discord.clusters.firstOrNull { shardId in it.minShard..it.maxShard }
        return shard ?: throw IllegalArgumentException("Shard $shardId not found in any cluster")
    }

    fun getShardIdFromGuildId(id: Long, totalShards: Int) = (id shr 22).rem(totalShards).toInt()

    suspend fun OrbitInstance.getMemberFromGuild(
        instance: OrbitInstance,
        guildId: String,
        memberId: Long
    ): CustomMemberResponse? {
        val shardId = getShardIdFromGuildId(guildId.toLong(), instance.config.discord.totalShards)
        val cluster = getClusterByShardId(instance, shardId)
        val memberAsUser = instance.shardManager.retrieveUserById(memberId).await()

        return if (cluster.id == instance.currentCluster.id) {
            instance.shardManager.getGuildById(guildId)?.let {
                val isMember = it.isMember(memberAsUser)

                return CustomMemberResponse(isMember = isMember)
            }
        } else {
            val fetchedInfo = getFromAnotherCluster(instance, cluster, "/api/v1/guilds/$guildId/$memberId")

            return if (fetchedInfo == null) {
                null
            } else {
                json.decodeFromString(fetchedInfo)
            }
        }
    }

    suspend fun OrbitInstance.getGuildInfo(instance: OrbitInstance, guildId: Long): CustomGuildInfo? {
        val shardId = getShardIdFromGuildId(guildId, instance.config.discord.totalShards)
        val cluster = getClusterByShardId(instance, shardId)

        return if (cluster.id == instance.currentCluster.id) {
            instance.shardManager.getGuildById(guildId)?.let {
                val availableStaticEmojis = it.emojis.filter { emoji -> emoji.isAvailable && !emoji.isAnimated }
                val guildOwner = it.retrieveOwner().await()

                CustomGuildInfo(
                    id = it.idLong,
                    name = it.name,
                    icon = it.iconUrl,
                    owner = CustomGuildInfo.GuildOwner(
                        id = guildOwner.idLong,
                        username = guildOwner?.user?.name ?: "Unknown",
                        discriminator = guildOwner?.user?.discriminator ?: "0000",
                        avatar = guildOwner?.user?.effectiveAvatarUrl
                    ),
                    textChannels = it.textChannels.map { channel -> channel.name },
                    voiceChannels = it.voiceChannels.map { channel -> channel.name },
                    roles = it.roles.map { role -> role.name },
                    emojis = it.emojis.map { emoji -> emoji.name },
                    memberCount = it.memberCount,
                    boostCount = it.boostCount,
                    splashUrl = it.splashUrl,
                    shardId = getShardIdFromGuildId(it.idLong, instance.config.discord.totalShards),
                    createdAt = it.timeCreated.toEpochSecond(),
                    joinedAt = it.selfMember.timeJoined.toEpochSecond(),
                    firstEmojis = availableStaticEmojis.take(30).map { emoji -> "<:${emoji.name}:${emoji.id}>" },
                    clusterInfo = cluster,
                    invites = it.retrieveInvites().await().map { invite ->
                        CustomGuildInfo.Invite(
                            url = invite.url,
                            maxAge = invite.maxAge,
                            maxUses = invite.maxUses,
                            temporary = invite.isTemporary,
                            uses = invite.uses,
                            createdBy = CustomGuildInfo.InviteOwner(
                                name = invite.inviter?.name ?: "Unknown",
                                id = invite.inviter?.id ?: "0"
                            )
                        )
                    }
                )
            }
        } else {
            val fetchedInfo = getFromAnotherCluster(instance, cluster, "/api/v1/guilds/$guildId")
            if (fetchedInfo == null) {
                return null
            } else {
                return json.decodeFromString(fetchedInfo)
            }
        }
    }

    suspend fun OrbitInstance.getMemberRolesFromGuildOrCluster(
        instance: OrbitInstance,
        guildId: Long,
        memberId: Long
    ): List<String> {
        val shardId = getShardIdFromGuildId(guildId, instance.config.discord.totalShards)
        val cluster = getClusterByShardId(instance, shardId)
        val guildShardId = getShardIdFromGuildId(guildId, instance.config.discord.totalShards)
        val guildCluster = getClusterByShardId(instance, guildShardId)
        val cacheKey = "$guildId:$memberId"
        val rolesResponse = cachedRoles.getIfPresent(cacheKey)

        if (guildCluster.id == instance.currentCluster.id) {
            logger.debug { "Fetching member roles from current cluster for guild $guildId and member $memberId" }
            val guild = instance.shardManager.getGuildById(guildId)
            if (guild != null) {
                val member = try {
                    guild.retrieveMemberById(memberId).await()
                } catch (_: Exception) {
                    return emptyList()
                }

                val roles = member.roles.map { it.id }
                cachedRoles.put(cacheKey, roles)
                return roles
            } else {
                return emptyList()
            }
        }

        if (rolesResponse != null) {
            // Return cached roles to avoid unnecessary API calls
            return rolesResponse
        } else {
            try {
                // Fetch roles from another cluster
                val roles = getFromAnotherCluster(instance, cluster, "/api/v1/guilds/$guildId/users/$memberId/roles")
                if (roles == null) {
                    return emptyList()
                } else {
                    val rolesToJSON = json.decodeFromString<List<Long>>(roles)
                    val rolesAsString = rolesToJSON.map { it.toString() }
                    cachedRoles.put(cacheKey, rolesAsString)
                    return rolesAsString
                }
            } catch (e: Exception) {
                logger.error(e) { e.message }
                return emptyList()
            }
        }
    }

    suspend fun OrbitInstance.getClusterInfo(cluster: OrbitConfig.DiscordSettings.Cluster): ClusterInfo? {
        return try {
            val jsonString = getFromAnotherCluster(this, cluster, "/api/v1/info") ?: return null
            json.decodeFromString<ClusterInfo>(jsonString)
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun getFromAnotherCluster(
        instance: OrbitInstance,
        cluster: OrbitConfig.DiscordSettings.Cluster,
        endpoint: String
    ): String? {
        return withContext(instance.coroutineDispatcher) {
            logger.info { "Fetching data from ${cluster.clusterUrl}" }
            val response = instance.http.get {
                url(cluster.clusterUrl + endpoint)
                header("Content-Type", "application/json")
                header("Authorization", "Bearer ${instance.config.internal.apiKey}")
                timeout {
                    connectTimeoutMillis = instance.config.discord.clusterConnectionTimeout
                    requestTimeoutMillis = instance.config.discord.clusterReadTimeout
                }
            }
            if (response.status != HttpStatusCode.OK) return@withContext null
            response.bodyAsText()
        }
    }
}