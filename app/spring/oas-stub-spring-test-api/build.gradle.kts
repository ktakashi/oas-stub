plugins {
    `java-library`
    kotlin("kapt")
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

group = "$group.spring"
description = "OAS Stub Spring test API"

dependencies {
    api(project(":lib:oas-stub-engine"))
    implementation(libs.spring.core)
    implementation(libs.spring.boot.core)
    implementation(libs.annotation.api)
    compileOnly(libs.jackson.annotations)

    kapt(libs.spring.boot.configuration.processor)
}
