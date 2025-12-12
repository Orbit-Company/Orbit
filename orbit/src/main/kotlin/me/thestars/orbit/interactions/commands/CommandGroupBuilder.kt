package me.thestars.orbit.interactions.commands

import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType

class CommandGroupBuilder(
    val name: String,
    val description: String
) {
    val subCommands = mutableListOf<CommandDeclarationBuilder>()

    fun subCommand(
        name: String,
        description: String,
        isPrivate: Boolean = false,
        category: String,
        availableForEarlyAccess: Boolean = false,
        aliases: List<String> = emptyList(),
        supportsLegacy: Boolean = false,
        integrationType: List<IntegrationType> = listOf(IntegrationType.GUILD_INSTALL),
        interactionContexts: List<InteractionContextType> = listOf(InteractionContextType.GUILD),
        block: CommandDeclarationBuilder.() -> Unit
    ) {
        val subCommand = CommandDeclarationBuilder(
            name,
            description,
            aliases,
            integrationType,
            interactionContexts
        )
        subCommand.block()
        subCommands.add(subCommand)
    }

    fun getSubCommand(name: String): CommandDeclarationBuilder? {
        return subCommands.find { it.name == name }
    }
}