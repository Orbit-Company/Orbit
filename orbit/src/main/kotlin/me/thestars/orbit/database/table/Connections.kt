package me.thestars.orbit.database.table

import me.thestars.orbit.utils.common.Enums
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption

object Connections : UUIDTable("connections") {
    val name = varchar("name", 128)
    val iconUrl = varchar("iconUrl", 256).nullable()
    val description = text("description").nullable()
    val invite = varchar("invite", 255).nullable()
    val type = integer(name = "type")
    val language = varchar("language", 255).default("pt-br")

    val paused = bool("paused").nullable()
    val pauseAt = long("pause_at").clientDefault { System.currentTimeMillis() }
    val locked = bool("locked").nullable()
    val lockedAt = long("locked_at").clientDefault { System.currentTimeMillis() }

    val creatorId = long("creator_id").nullable()
    val guildCreatorId = long("guild_creator_id").nullable()
    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }
    val flags = integer("flags").default(0)

    val channelId = long("channel_id")
    val logsType = text("logs_type").default("[]")
    val logsChannelId = long("logs_channel_id").nullable()

    val messageComponentType = integer("message_component_type").default(1)

    val guild = reference("guild_id", Guilds, onDelete = ReferenceOption.CASCADE)
}

object ConnectionInvites : UUIDTable("connection_invites") {
    val code = varchar("code", 255).uniqueIndex()
    val connection = reference("connection_id", Connections, onDelete = ReferenceOption.CASCADE)
    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }
    val expiresAt = long("expires_at").nullable()
    val maxUses = integer("max_uses").nullable()
    val uses = integer("uses").default(0)
    val createdBy = uuid("created_by")
}

object ModerationRules : UUIDTable("moderation_rules") {
    val connection = reference("connection_id", Connections, onDelete = ReferenceOption.CASCADE)
    val type = enumerationByName("type", 20, Enums.ModerationRuleType::class)
    val pattern = text("pattern")
    val action = enumerationByName("action", 30, Enums.ModerationAction::class).default(Enums.ModerationAction.DELETE_MESSAGE)
    val reason = text("reason").nullable()
    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }
}

object Categories : UUIDTable("categories") {
    val name = varchar("name", 200).uniqueIndex()
    val slug = varchar("slug", 200).uniqueIndex()
}