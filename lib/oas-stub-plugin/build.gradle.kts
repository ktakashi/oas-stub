plugins {
    `java-library`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

description = "OAS stub plugin APIs"

dependencies {
    implementation(libs.kotlin.stdlib)
    api(libs.servlet.api)
    api(libs.jackson.databind.core)
}
