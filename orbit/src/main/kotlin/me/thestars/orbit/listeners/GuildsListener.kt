package me.thestars.orbit.listeners

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.thestars.orbit.OrbitInstance
import me.thestars.orbit.utils.common.Constants
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class GuildsListener(private val instance: OrbitInstance) : ListenerAdapter() {
    private val logger = KotlinLogging.logger {}
    private val coroutineScope = CoroutineScope(instance.coroutineDispatcher + SupervisorJob())

    override fun onReady(event: ReadyEvent) {
        coroutineScope.launch {
            event.jda.presence.activity = Activity.customStatus(
                Constants.setOrbitActivity(
                    "☄️ Coming Soon...",
                    instance.config.environment,
                    instance.currentCluster.id,
                    event.jda.shardManager?.shards?.size ?: 1
                )
            )
        }
    }
}