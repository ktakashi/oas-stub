plugins {
    `java-library`
    `maven-publish`
    signing
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.dokka.get().pluginId)
    id("io.github.ktakashi.oas.conventions")
}

group = "$group.plugins"
description = "OAS stub plugin APIs"

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    api(libs.servlet.api)
}
