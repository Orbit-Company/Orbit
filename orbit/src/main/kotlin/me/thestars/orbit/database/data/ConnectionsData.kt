package me.thestars.orbit.database.data

import me.thestars.orbit.database.dao.ConnectionsEntity
import me.thestars.orbit.database.dao.GuildsEntity
import me.thestars.orbit.database.table.Connections
import net.dv8tion.jda.api.interactions.DiscordLocale
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun ConnectionsEntity.Companion.FindByName(name: String, channelId: Long): ConnectionsEntity? {
    return newSuspendedTransaction {
        ConnectionsEntity.find { (Connections.name eq name) and (Connections.channelId eq channelId) }.firstOrNull()
    }
}

suspend fun ConnectionsEntity.Companion.ExistsConnection(name: String): ConnectionsEntity? {
    return newSuspendedTransaction {
        ConnectionsEntity.find { Connections.name eq name }.firstOrNull()
    }
}

suspend fun ConnectionsEntity.Companion.CreateConnection(
    name: String,
    guild: GuildsEntity,
    creatorId: Long,
    description: String? = null,
    invite: String? = null,
    channelId: Long? = null,
    logsChannelId: Long? = null,
    type: Int? = null,
    flags: Int,
    language: DiscordLocale,
    messageComponentType: Int? = null,
): ConnectionsEntity {
    return newSuspendedTransaction {
        ConnectionsEntity.new {
            this.name = name
            this.guild = guild
            this.creatorId = creatorId
            if (description != null) this.description = description
            if (invite != null) this.invite = invite
            if (channelId != null) this.channelId = channelId
            if (logsChannelId != null) this.logsChannelId = logsChannelId
            if (type != null) this.type = type
            this.language = language.toString()
            if (messageComponentType != null) this.messageComponentType = messageComponentType
            this.flags = flags
            this.createdAt = System.currentTimeMillis()
        }
    }
}