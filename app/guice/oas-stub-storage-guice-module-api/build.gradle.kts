plugins {
    `java-library`
     id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

group = "$group.guice"
description = "OAS stub storage Guice modules API"

dependencies {
    implementation(libs.guice.core)
    implementation(libs.kotlin.stdlib)
}
