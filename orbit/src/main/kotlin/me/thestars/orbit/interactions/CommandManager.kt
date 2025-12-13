package me.thestars.orbit.interactions

import dev.minn.jda.ktx.coroutines.await
import me.thestars.orbit.OrbitInstance
import me.thestars.orbit.interactions.commands.CommandDeclarationWrapper
import me.thestars.orbit.interactions.commands.CommandUnleashed
import me.thestars.orbit.interactions.vanilla.common.declarations.OrbitCommand
import me.thestars.orbit.utils.ClusterUtils
import me.thestars.orbit.utils.common.Constants
import mu.KotlinLogging
import net.dv8tion.jda.api.interactions.commands.Command

class CommandManager(private val instance: OrbitInstance) {
    val commands = mutableListOf<CommandDeclarationWrapper>()
    private val logger = KotlinLogging.logger { }

    operator fun get(name: String): CommandDeclarationWrapper? {
        return commands.find { it.create().name == name }
    }

    fun getCommandAsLegacy(commandName: String): CommandUnleashed? {
        commands.forEach { wrapper ->
            val cmd = wrapper.create()

            cmd.subCommands.forEach { subCmd ->
                if (subCmd.name.equals(commandName, ignoreCase = true) || subCmd.aliases.any {
                        it.equals(commandName, ignoreCase = true)
                    }) {
                    return CommandUnleashed(subCmd.executor ?: cmd.executor, subCmd)
                }
            }

            if (cmd.name.equals(commandName, ignoreCase = true) || cmd.aliases.any {
                    it.equals(commandName, ignoreCase = true)
                }) {
                if (cmd.executor != null) {
                    return CommandUnleashed(cmd.executor, cmd)
                }
            }
        }
        return null
    }


    private fun register(command: CommandDeclarationWrapper) {
        commands.add(command)
    }

    suspend fun handle(): MutableList<Command> {
        val allCommands = mutableListOf<Command>()

        val supportServerShardId = ClusterUtils.getShardIdFromGuildId(
            Constants.SUPPORT_SERVER_ID,
            instance.config.discord.totalShards
        )

        instance.shardManager.shards.forEach { shard ->
            val action = shard.updateCommands()

            commands.forEach { command ->
                val builtCommand = command.create().build()

                action.addCommands(builtCommand)
            }

            val registeredCommands = action.await()
            allCommands.addAll(registeredCommands)
            logger.info { "${commands.size} commands registered on shard #${shard.shardInfo.shardId}" }
        }

        return allCommands
    }


    init {
        register(OrbitCommand())
    }
}