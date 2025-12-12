package me.thestars.orbit.utils.serializable

import kotlinx.serialization.Serializable

@Serializable
data class OrbitConfig(
    val environment: String,
    val discord: DiscordSettings,
    val database: DatabaseSettings,
    val internal: InternalSettings
) {
    @Serializable
    data class InternalSettings(
        val apiKey: String
    )

    @Serializable
    data class DatabaseSettings(
        val url: String,
        val username: String,
        val password: String
    )

    @Serializable
    data class DiscordSettings(
        val ownerId: Long,
        val guildId: Long,
        val token: String,
        val totalShards: Int,
        val getClusterIdFromHostname: Boolean,
        val baseUrl: String?,
        val clusterReadTimeout: Long,
        val clusterConnectionTimeout: Long,
        val replicaId: Int,
        val clusters: List<Cluster>
    ) {
        @Serializable
        data class Cluster(
            val id: Int,
            val name: String,
            val minShard: Int,
            val maxShard: Int,
            val isMasterCluster: Boolean,
            val clusterUrl: String
        )
    }
}