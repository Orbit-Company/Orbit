package me.thestars.orbit.interactions.vanilla.connections

import dev.minn.jda.ktx.messages.EmbedBuilder
import me.thestars.orbit.database.dao.ConnectionsEntity
import me.thestars.orbit.database.data.CountConnectionsByName
import me.thestars.orbit.database.data.FindByName
import me.thestars.orbit.database.data.FindConnectionsByChannel
import me.thestars.orbit.interactions.commands.CommandContext
import me.thestars.orbit.interactions.commands.UnleashedCommandExecutor
import me.thestars.orbit.interactions.pretty
import me.thestars.orbit.utils.common.OrbitEmotes
import java.time.Instant

class ConnectionInformationExecutor : UnleashedCommandExecutor() {
    override suspend fun execute(context: CommandContext) {
        val connectionName = context.getOption("connection", 0, String::class.java, true)
        val connectionData = connectionName?.let {
            ConnectionsEntity.FindByName(it, context.event.channel?.idLong!!)
        } ?: run {
            val connectionsInChannel = ConnectionsEntity.FindConnectionsByChannel(context.event.channel?.idLong!!)
            when {
                connectionsInChannel.isEmpty() -> {
                    context.reply(ephemeral = true) {
                        content = pretty(
                            OrbitEmotes.error,
                            context.locale["generics.error.channelIsEmpty", context.user.asMention]
                        )
                    }
                    return
                }

                connectionsInChannel.size > 1 -> {
                    context.reply(ephemeral = true) {
                        content = pretty(
                            OrbitEmotes.error,
                            context.locale["generics.error.moreOfOneConnectionChannel", context.user.asMention]
                        )
                    }
                    return
                }

                else -> connectionsInChannel.first()
            }
        }

        val creatorUser = try {
            connectionData.creatorId?.let { id -> context.jda.getUserById(id) }
        } catch (_: Exception) {
            null
        }

        val creatorGuild = try {
            connectionData.guildCreatorId?.let { id -> context.jda.getGuildById(id) }
        } catch (_: Exception) {
            null
        }

        val connectedChannels = ConnectionsEntity.CountConnectionsByName(connectionData.name)
        val paused = connectionData.paused == true
        val locked = connectionData.locked == true
        val displayName = connectionData.name
        val displayIcon = connectionData.iconUrl
        val displayDescription = connectionData.description
        val displayType = mapType(connectionData.type, context)
        val displayLanguage = connectionData.language
        val displayCreator = creatorUser?.asTag ?: connectionData.creatorId?.toString() ?: context.locale["generics.text.unknown"]
        val displayGuildCreator = creatorGuild?.name ?: connectionData.guildCreatorId?.toString() ?: context.locale["generics.text.unknown"]
        val createdAtTimestamp = connectionData.createdAt.let { "<t:${it / 1000}:R>" }

        val statusValue =
            (if (paused) context.locale["generics.text.inactive"] else context.locale["generics.text.active"]) + if (locked) " • 🔒 ${context.locale["generics.text.blocked"]}" else ""

        val embed = EmbedBuilder {
            title = "<:chain:1452471661885657239> ${context.locale["connections.info.title", displayName]}"
            description =
                "${context.locale["connections.info.description", context.user.asMention]}\n\n<:book:1452485997441978388> **${context.locale["connections.info.connectionDescription"]}:**\n> " + "`${(displayDescription ?: context.locale["connections.info.notDescription"])}`"
            field {
                name = "<:padlock:1452471991264346255> ${context.locale["connections.info.connectionType"]}"
                value = "> `$displayType`"
                inline = true
            }
            field {
                name = "${OrbitEmotes.world} ${context.locale["connections.info.connectionLanguage"]}"
                value = "> `${displayLanguage.replace("PORTUGUESE_BRAZILIAN", context.locale["generics.text.portuguese"]).replace("ENGLISH_US", context.locale["generics.text.english"])}`"
                inline = true
            }
            field {
                name = "${OrbitEmotes.calendar} ${context.locale["connections.info.connectionCreated"]}"
                value = "> $createdAtTimestamp"
                inline = false
            }
            field {
                name = "<:satelite:1452474813019979979> ${context.locale["connections.info.connectionStatus"]}"
                value = "> `$statusValue`"
                inline = true
            }
            field {
                name = "${OrbitEmotes.crown} ${context.locale["connections.info.connectionOwner"]}"
                value = "> `$displayCreator`"
                inline = true
            }
            field {
                name = "<:connections:1452475813751427134> ${context.locale["connections.info.connectionCount"]}"
                value = "> `${connectedChannels}`"
                inline = false
            }
            field {
                name = "<:light:1452797363578994839> ${context.locale["connections.info.connectionCuriosity"]}"
                value = "> ${context.locale["connections.info.curiosity", displayGuildCreator]}"
            }
            footer {
                name = context.locale["connections.info.footer", context.user.name]
                iconUrl = context.user.avatarUrl
            }
            timestamp = Instant.now()
        }

        if (!displayIcon.isNullOrBlank()) {
            embed.thumbnail = displayIcon
        } else {
            embed.thumbnail = "https://media.discordapp.net/attachments/1452477958332878930/1452477971553194025/ABS2GSmmgckOX9qpP9ba3FLrRbaE7BI31bCZclNynCWgWTVyqw_sz7KR3EDlA4vlZ-e5j2Hc-kqpSrXgh6MZe1E-jw2iy0BKul_XAfCUGezRdYOr-cu-CO-v39aNEuR_AfZyVmlnDeIFQAl-tWPMhC5bYqGETgvIfOEbpOzZvGG5e-Trad40Xgs1024-rj.png?ex=6949f506&is=6948a386&hm=00bbb299afdd794f8dd4983132db2a508d128875b2f303a4922779f05e944f7f&=&format=webp&quality=lossless&width=648&height=648"
        }

        embed.color = 0x5D3FD3

        context.reply {
            embeds += embed.build()
        }
    }

    private fun mapType(type: Int?, context: CommandContext): String {
        return when (type) {
            0 -> context.locale["generics.text.unknow"]
            1 -> context.locale["generics.text.public"]
            2 -> context.locale["generics.text.guest"]
            3 -> context.locale["generics.text.private"]
            else -> context.locale["generics.text.other"]
        }
    }
}
