plugins {
    `java-library`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

group = "$group.spring"
description = "OAS stub storage autoconfigure API"

dependencies {
    api(project(":lib:oas-stub-storage-api"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.spring.boot.autoconfigure)
}
