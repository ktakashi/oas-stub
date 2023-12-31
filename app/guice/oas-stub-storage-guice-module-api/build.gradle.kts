plugins {
    `java-library`
     id(libs.plugins.kotlin.jvm.get().pluginId)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

group = "$group.guice"
description = "OAS stub storage Guice modules API"

dependencies {
    implementation(libs.guice.core)
    implementation(libs.kotlin.stdlib)
    implementation(libs.jackson.databind.core)
}
