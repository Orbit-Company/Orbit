package me.thestars.orbit.utils

import me.thestars.orbit.OrbitInstance
import java.time.LocalTime

object TasksUtils {
    fun launchTasks(instance: OrbitInstance) {}

    private fun at(hour: Int, minute: Int) = LocalTime.of(hour, minute)
}