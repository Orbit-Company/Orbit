plugins {
    java
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow")
    application
}

dependencies {
    implementation(libs.kotlin.stdlib.jdk8)

    // Discord
    implementation(libs.jda)
    implementation("com.github.freya022:jda-ktx:8929de93af")

    // Coroutines and DateTime
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.debug)
    implementation(libs.kotlinx.datetime)

    // Database
    implementation(libs.hikari)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.postgres)

    // Ktor
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.htmx)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)

    // Serialization
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.hocon)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.module.kotlin)

    // Logging
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging)

    // Caching
    implementation(libs.caffeine)

    // Thread
    implementation(libs.guava)
}

tasks.test {
    jvmArgs = listOf("--enable-native-access=ALL-UNNAMED", "-XX:+EnableDynamicAgentLoading")

    useJUnitPlatform()
}

application {
    mainClass.set("me.thestars.orbit.OrbitLauncher")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(Versions.JVM_TARGET))
    }
}