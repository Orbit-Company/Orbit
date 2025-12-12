package me.thestars.orbit.utils

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.hocon.Hocon

object Constants {
    @OptIn(ExperimentalSerializationApi::class)
    val HOCON = Hocon { useArrayPolymorphism = true }

    const val SUPPORT_SERVER_ID = 1210032940709318678
}

data object OrbitEmotes {
    const val error = "❌"
}