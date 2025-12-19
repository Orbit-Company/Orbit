package me.thestars.orbit.interactions.commands

import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType

interface CommandDeclarationWrapper {
    fun create(): CommandDeclarationBuilder

    fun slashCommand(
        name: String,
        description: String = "No description provided",
        aliases: List<String> = emptyList(),
        block: CommandDeclarationBuilder.() -> Unit
    ): CommandDeclarationBuilder {
        return CommandDeclarationBuilder(
            name,
            description,
            aliases,
            integrationType = listOf(IntegrationType.GUILD_INSTALL),
            interactionContexts = listOf(InteractionContextType.GUILD)
        ).apply(block)
    }
}