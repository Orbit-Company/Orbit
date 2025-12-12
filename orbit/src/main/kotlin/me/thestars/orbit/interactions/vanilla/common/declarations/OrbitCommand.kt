package me.thestars.orbit.interactions.vanilla.common.declarations

import me.thestars.orbit.interactions.commands.CommandDeclarationWrapper
import me.thestars.orbit.interactions.vanilla.common.PingExecutor
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType

class OrbitCommand : CommandDeclarationWrapper {
    override fun create() = slashCommand("orbit") {
        interactionContexts = listOf(
            InteractionContextType.BOT_DM,
            InteractionContextType.GUILD,
            InteractionContextType.PRIVATE_CHANNEL
        )

        integrationType = listOf(IntegrationType.USER_INSTALL, IntegrationType.GUILD_INSTALL)

        subCommand("ping") {
            aliases = listOf("ping")
            executor = PingExecutor()
        }
    }
}