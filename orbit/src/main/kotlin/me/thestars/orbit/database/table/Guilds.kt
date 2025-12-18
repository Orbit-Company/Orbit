package me.thestars.orbit.database.table

import org.jetbrains.exposed.dao.id.LongIdTable

object Guilds : LongIdTable("guilds") {
    val prefix = varchar("prefix", 2).default("!")
    val cases = text("cases").nullable()
    val language = varchar("language", 10).default("pt-br")
    val createdAt = long("created_at").clientDefault { System.currentTimeMillis() }
}