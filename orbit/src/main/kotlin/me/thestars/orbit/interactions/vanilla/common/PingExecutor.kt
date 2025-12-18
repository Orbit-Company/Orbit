package me.thestars.orbit.interactions.vanilla.common

import me.thestars.orbit.interactions.commands.CommandContext
import me.thestars.orbit.interactions.commands.UnleashedCommandExecutor

class PingExecutor : UnleashedCommandExecutor() {
    override suspend fun execute(context: CommandContext) {
        val gatewayPing = context.jda.gatewayPing
        val currentShardId = context.jda.shardInfo.shardId
        val totalShards = context.jda.shardInfo.shardTotal
        val currentClusterId = context.orbit.currentCluster.id
        val currentClusterName = context.orbit.currentCluster.name

        context.reply {
            content =
                context.locale["ping.response", currentShardId.toString(), gatewayPing.toString(), currentShardId.toString(), totalShards.toString(), currentClusterId.toString(), currentClusterName]
        }
    }
}