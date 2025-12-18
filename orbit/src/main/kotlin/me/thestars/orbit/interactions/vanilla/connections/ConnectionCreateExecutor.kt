package me.thestars.orbit.interactions.vanilla.connections

import me.thestars.orbit.interactions.commands.CommandContext
import me.thestars.orbit.interactions.commands.UnleashedCommandExecutor
import me.thestars.orbit.interactions.pretty
import me.thestars.orbit.utils.common.OrbitEmotes

class ConnectionCreateExecutor : UnleashedCommandExecutor() {
    override suspend fun execute(context: CommandContext) {
        if (context.guild == null) {
            context.reply(ephemeral = true) {
                content = pretty(
                    OrbitEmotes.error,
                    context.locale["generics.error.notInGuild", context.user.asMention]
                )
            }

            return
        }

        context.reply {
            content = pretty(
                OrbitEmotes.success,
                context.locale["connections.create.response", context.user.asMention, context.guild?.idLong.toString()]
            )
        }
    }
}