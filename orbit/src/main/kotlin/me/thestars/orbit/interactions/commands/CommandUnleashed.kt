package me.thestars.orbit.interactions.commands

data class CommandUnleashed(
    val executor: UnleashedCommandExecutor?,
    val command: CommandDeclarationBuilder
)