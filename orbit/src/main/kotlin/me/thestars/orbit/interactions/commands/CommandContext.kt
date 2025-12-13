package me.thestars.orbit.interactions.commands

import dev.minn.jda.ktx.messages.InlineMessage
import me.thestars.orbit.OrbitInstance
import me.thestars.orbit.utils.common.OrbitLocale
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.modals.Modal
import net.dv8tion.jda.api.interactions.modals.ModalMapping

interface CommandContext {
    val jda: JDA
    val user: User
    val guild: Guild?
    val orbit: OrbitInstance
    val locale: OrbitLocale
    val event: GenericEvent

    suspend fun reply(ephemeral: Boolean = false, block: InlineMessage<*>.() -> Unit)
    suspend fun defer(ephemeral: Boolean = false): InteractionHook?
    suspend fun deferEdit(): InteractionHook?
    suspend fun edit(block: InlineMessage<*>.() -> Unit): Unit?
    suspend fun sendModal(modal: Modal): Void?
    fun getValue(name: String): ModalMapping?
    fun <T> getOption(name: String, argNumber: Int = 0, type: Class<T>, isFullString: Boolean = false): T?
}