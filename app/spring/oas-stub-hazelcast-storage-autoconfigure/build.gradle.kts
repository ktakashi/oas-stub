plugins {
    `java-library`
    kotlin("kapt")
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

group = "$group.spring"
description = "OAS stub hazelcast storage starter"

dependencies {
    api(project(":app:spring:oas-stub-storage-autoconfigure-api"))
    api(project(":lib:storages:oas-stub-hazelcast-storage"))
    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.kotlin.stdlib)
    implementation(libs.jackson.databind)
    compileOnly(libs.hazelcast)

    kapt(libs.spring.boot.configuration.processor)
}
