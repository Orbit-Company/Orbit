package me.thestars.orbit.database.data

import me.thestars.orbit.database.dao.GuildsEntity
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun GuildsEntity.Companion.CreateOrGet(id: Long): GuildsEntity {
    return newSuspendedTransaction {
        GuildsEntity.findById(id) ?: GuildsEntity.new(id) { }
    }
}