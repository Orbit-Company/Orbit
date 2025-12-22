package me.thestars.orbit.database.dao

import me.thestars.orbit.database.table.Categories
import me.thestars.orbit.database.table.ConnectionInvites
import me.thestars.orbit.database.table.Connections
import me.thestars.orbit.database.table.ModerationRules
import net.dv8tion.jda.api.entities.Guild
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID

class ConnectionsEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ConnectionsEntity>(me.thestars.orbit.database.table.Connections)

    var name by Connections.name
    var iconUrl by Connections.iconUrl
    var description by Connections.description
    var invite by Connections.invite
    var type by Connections.type
    var language by Connections.language

    var paused by Connections.paused
    var pauseAt by Connections.pauseAt
    var locked by Connections.locked
    var lockedAt by Connections.lockedAt

    var creatorId by Connections.creatorId
    val guildCreatorId by Connections.guildCreatorId
    var createdAt by Connections.createdAt
    var flags by Connections.flags

    var channelId by Connections.channelId
    var logsType by Connections.logsType
    var logsChannelId by Connections.logsChannelId

    var messageComponentType by Connections.messageComponentType
    var guild by GuildsEntity referencedOn Connections.guild


    val moderationRules by ModerationRule referrersOn ModerationRules.connection
}

class ConnectionInvite(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ConnectionInvite>(ConnectionInvites)

    var code by ConnectionInvites.code
    var connection by ConnectionsEntity referencedOn ConnectionInvites.connection
    var createdAt by ConnectionInvites.createdAt
    var expiresAt by ConnectionInvites.expiresAt
    var maxUses by ConnectionInvites.maxUses
    var uses by ConnectionInvites.uses
    var createdBy by ConnectionInvites.createdBy
}

class ModerationRule(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ModerationRule>(ModerationRules)

    var connection by ConnectionsEntity referencedOn ModerationRules.connection
    var type by ModerationRules.type
    var pattern by ModerationRules.pattern
    var action by ModerationRules.action
    var reason by ModerationRules.reason
    var createdAt by ModerationRules.createdAt
}

class Category(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<Category>(Categories)

    var name by Categories.name
    var slug by Categories.slug

    val connections by ConnectionsEntity via Connections // placeholder; use referrers or explicit join if needed
}