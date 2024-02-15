plugins {
    `java-library`
    kotlin("plugin.serialization") version "1.9.10"
    id("io.ktor.plugin") version "2.3.8"
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

group = "$group.ktor"
description = "OAS stub Ktor web"

dependencies {
    api(project(":lib:oas-stub-engine"))
    implementation(libs.ktor.server.core)
    implementation(libs.koin.ktor)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.logging)
    testImplementation(libs.ktor.server.content.negotiation)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.kotlinx.json)
    testImplementation(libs.logback.classic)
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.junit5)
    testImplementation(libs.mockito.core)
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
