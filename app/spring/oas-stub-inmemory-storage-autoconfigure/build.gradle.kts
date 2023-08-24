plugins {
    `java-library`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

group = "$group.spring"
description = "OAS stub in-memory storage starter"

dependencies {
    api(project(":lib:storages:oas-stub-inmemory-storage"))
    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.kotlin.stdlib)
}
