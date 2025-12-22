package me.thestars.orbit.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.thestars.orbit.database.table.Categories
import me.thestars.orbit.database.table.ConnectionInvites
import me.thestars.orbit.database.table.Connections
import me.thestars.orbit.database.table.Guilds
import me.thestars.orbit.database.table.ModerationRules
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

data class PostgresConfig(
    val url: String,
    val username: String,
    val password: String
)

class DatabaseService {
    private var hikari: HikariDataSource? = null

    fun connect(postgres: PostgresConfig) {
        val config = HikariConfig().apply {
            jdbcUrl = "jdbc:${postgres.url}"
            driverClassName = "org.postgresql.Driver"
            username = postgres.username
            password = postgres.password
            maximumPoolSize = 8
        }

        hikari = HikariDataSource(config)
        Database.connect(hikari!!)

        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                Guilds,
                Connections,
                ConnectionInvites,
                ModerationRules,
                Categories
            )
        }
    }

    fun close() {
        hikari?.close()
        hikari = null
    }
}