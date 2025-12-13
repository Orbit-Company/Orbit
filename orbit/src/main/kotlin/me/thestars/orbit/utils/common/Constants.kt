package me.thestars.orbit.utils.common

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.Hocon

object Constants {
    @OptIn(ExperimentalSerializationApi::class)
    val HOCON = Hocon { useArrayPolymorphism = true }

    const val SUPPORT_SERVER_ID = 1210032940709318678

    fun setOrbitActivity(
        activity: String,
        environment: String,
        clusterId: Int?,
        shards: Int
    ): String {
        return if (clusterId != null) {
            when (environment) {
                "development" -> "☄️ Coming Soon... | Cluster $clusterId ($shards)"
                "production" -> "$activity | Cluster $clusterId ($shards)"
                else -> "$activity | Cluster $clusterId ($shards)"
            }
        } else {
            when (environment) {
                "development" -> "☄️ Coming Soon..."
                "production" -> activity
                else -> activity
            }
        }
    }
}

data object OrbitEmotes {
    const val error = "❌"
}