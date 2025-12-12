package me.thestars.orbit.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

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
    }

    fun close() {
        hikari?.close()
        hikari = null
    }
}