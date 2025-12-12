package me.thestars.orbit.utils.serializable.cluster

import kotlinx.serialization.Serializable

@Serializable
data class CustomMemberResponse(
    val isMember: Boolean
)