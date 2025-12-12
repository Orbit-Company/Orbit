package me.thestars.orbit.interactions.commands

abstract class UnleashedCommandExecutor {
    abstract suspend fun execute(context: CommandContext)
}