package me.thestars.orbit.listeners

import dev.minn.jda.ktx.coroutines.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.thestars.orbit.OrbitInstance
import me.thestars.orbit.interactions.InteractionCommandContext
import me.thestars.orbit.interactions.components.ComponentId
import me.thestars.orbit.interactions.pretty
import me.thestars.orbit.utils.common.OrbitEmotes
import mu.KotlinLogging
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import kotlin.system.measureTimeMillis

class InteractionsListener(private val instance: OrbitInstance) : ListenerAdapter() {
    private val coroutineScope = CoroutineScope(instance.coroutineDispatcher + SupervisorJob())
    private val logger = KotlinLogging.logger { }

    override fun onReady(event: ReadyEvent) {
        logger.info { "Shard #${event.jda.shardInfo.shardId} is ready!" }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        instance.threadPoolManager.launchMessageJob(event) {
            val commandName = event.fullCommandName.split(" ").first()
            val command = instance.commandHandler[commandName]?.create()

            if (command != null) {
                val context = InteractionCommandContext(event, instance)

                val subCommandGroupName = event.subcommandGroup
                val subCommandName = event.subcommandName

                val subCommandGroup =
                    if (subCommandGroupName != null)
                        command.getSubCommandGroup(subCommandGroupName) else null
                val subCommand = if (subCommandName != null) {
                    if (subCommandGroup != null) {
                        subCommandGroup.getSubCommand(subCommandName)
                    } else {
                        command.getSubCommand(subCommandName)
                    }
                } else null

                try {
                    val executionTime = measureTimeMillis {
                        if (subCommand != null) {
                            subCommand.executor?.execute(context)
                        } else if (subCommandGroupName == null && subCommandName == null) {
                            command.executor?.execute(context)
                        }
                    }

                    logger.info { "${context.user.name} (${context.user.id}) executed ${event.fullCommandName} in ${context.guild?.name} (${context.guild?.id}) in ${executionTime}ms" }
                } catch (e: Exception) {
                    logger.error(e) { "An error occurred while executing command: ${event.fullCommandName}" }
                    context.reply(true) {
                        content = pretty(
                            OrbitEmotes.error,
                            context.locale["commands.error", e.toString()]
                        )
                    }
                }
            }
        }
    }

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        coroutineScope.launch {
            val componentId = try {
                ComponentId(event.componentId)
            } catch (_: IllegalArgumentException) {
                logger.info { "Invalid component ID: ${event.componentId}" }
                return@launch
            }

            val callbackId = instance.interactionManager.componentCallbacks[componentId.uniqueId]
            val context = InteractionCommandContext(event, instance)

            if (callbackId == null) {
                event.editButton(
                    event.button.asDisabled()
                ).await()

                context.reply(true) {
                    content = pretty(
                        OrbitEmotes.error,
                        context.locale["commands.componentExpired"]
                    )
                }

                return@launch
            }

            callbackId.invoke(context)
        }
    }

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        coroutineScope.launch(instance.coroutineDispatcher) {
            val componentId = try {
                ComponentId(event.componentId)
            } catch (_: IllegalArgumentException) {
                logger.info { "Unknown component received" }
                return@launch
            }

            try {
                val callback = instance.interactionManager.stringSelectMenuCallbacks[componentId.uniqueId]
                val context = InteractionCommandContext(event, instance)

                if (callback == null) {
                    event.editSelectMenu(
                        event.selectMenu.asDisabled()
                    ).await()

                    context.reply(true) {
                        content = pretty(
                            OrbitEmotes.error,
                            context.locale["commands.componentExpired"]
                        )
                    }

                    return@launch
                }

                callback.invoke(context, event.values)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        coroutineScope.launch {
            val componentId = try {
                ComponentId(event.componentId)
            } catch (_: IllegalArgumentException) {
                logger.info { "Unknown component received" }
                return@launch
            }

            try {
                val callback = instance.interactionManager.entitySelectMenuCallbacks[componentId.uniqueId]
                val context = InteractionCommandContext(event, instance)

                if (callback == null) {
                    event.editSelectMenu(
                        event.selectMenu.asDisabled()
                    ).await()

                    context.reply(true) {
                        content = pretty(
                            OrbitEmotes.error,
                            context.locale["commands.componentExpired"]
                        )
                    }

                    return@launch
                }

                callback.invoke(context, event.values)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        coroutineScope.launch {
            logger.info { "Modal ${event.modalId} submitted by ${event.user.name} (${event.user.id})" }

            val modalId = try {
                ComponentId(event.modalId)
            } catch (e: IllegalArgumentException) {
                logger.info { "Invalid Modal ID: ${event.modalId}" }
                return@launch
            }

            val callbackId = instance.interactionManager.componentCallbacks[modalId.uniqueId]

            val context = InteractionCommandContext(event, instance)

            callbackId?.invoke(context)
        }
    }
}