package me.thestars.orbit.interactions.vanilla.connections

import me.thestars.orbit.database.dao.ConnectionsEntity
import me.thestars.orbit.database.dao.GuildsEntity
import me.thestars.orbit.database.data.CreateConnection
import me.thestars.orbit.database.data.CreateOrGet
import me.thestars.orbit.database.data.ExistsConnection
import me.thestars.orbit.database.data.FindByName
import me.thestars.orbit.interactions.commands.CommandContext
import me.thestars.orbit.interactions.commands.UnleashedCommandExecutor
import me.thestars.orbit.interactions.pretty
import me.thestars.orbit.utils.common.OrbitConnectionFlag
import me.thestars.orbit.utils.common.OrbitEmotes
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.interactions.DiscordLocale

class ConnectionJoinExecutor : UnleashedCommandExecutor() {
    override suspend fun execute(context: CommandContext) {
        val connectionName = context.getOption("connection", 0, String::class.java, true)
        val targetChannel = context.getOption("channel", 0, Channel::class.java) ?: context.event.channel

        if (context.guild == null) {
            context.reply(ephemeral = true) {
                content = pretty(
                    OrbitEmotes.error,
                    context.locale["generics.error.notInGuild", context.user.asMention]
                )
            }

            return
        }

        val userMemberPermissions = context.member!!.hasPermission(Permission.ADMINISTRATOR) ||
                context.member!!.hasPermission(Permission.MANAGE_GUILD_EXPRESSIONS)

        if (!userMemberPermissions) {
            context.reply(ephemeral = true) {
                content = pretty(
                    OrbitEmotes.error,
                    context.locale["generics.error.notUserPermissions", context.user.asMention, "ADMINISTRATOR, MANAGE_GUILD_PERMISSIONS"]
                )
            }

            return
        }

        val guildData = GuildsEntity.CreateOrGet(context.guild!!.idLong)
        val connectionInChannel = ConnectionsEntity.FindByName(connectionName!!, context.event.channel!!.idLong)

        if (connectionInChannel != null) {
            context.reply(ephemeral = true) {
                content = pretty(
                    OrbitEmotes.error,
                    context.locale["generics.error.connectionAsExist", context.user.asMention, connectionName]
                )
            }

            return
        }

        val totalConnectionsInGuild = guildData.orbitConnections.toList().size
        val limitConnectionsInGuild = 5

        if (totalConnectionsInGuild > limitConnectionsInGuild) {
            context.reply(ephemeral = true) {
                content = pretty(
                    OrbitEmotes.error,
                    context.locale["generics.error.limitConnections", context.user.asMention, limitConnectionsInGuild.toString()]
                )
            }

            return
        }

        val existsConnection = ConnectionsEntity.ExistsConnection(connectionName)
        if (existsConnection == null) {
            context.reply(ephemeral = true) {
                content = pretty(
                    OrbitEmotes.error,
                    context.locale["generics.error.connectionNotExists", context.user.asMention, connectionName]
                )
            }

            return
        }

        ConnectionsEntity.CreateConnection(
            name = connectionName,
            guild = guildData,
            creatorId = context.user.idLong,
            type = 2,
            channelId = targetChannel!!.idLong,
            language = (context.guild?.locale ?: "pt-br") as DiscordLocale,
            flags = combineFlags(
                OrbitConnectionFlag.AllowEmojis,
                OrbitConnectionFlag.AllowMentions,
                OrbitConnectionFlag.AllowOrigin,
                OrbitConnectionFlag.AllowFiles,
            )
        )

        context.reply {
            content = pretty(
                OrbitEmotes.success,
                context.locale["connections.join.response", context.user.asMention, connectionName]
            )
        }
    }
}

fun combineFlags(vararg flags: OrbitConnectionFlag): Int {
    return flags.fold(0) { acc, flag -> acc or flag.value }
}