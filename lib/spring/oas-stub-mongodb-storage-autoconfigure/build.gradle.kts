import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    `java-library`
    kotlin("kapt")
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

group = "$group.spring"
description = "OAS stub mongodb storage starter"

tasks.withType<DokkaTask>().configureEach {
    tasks.findByName("kaptKotlin")?.let { task -> dependsOn(task)}
}

dependencies {
    api(project(":lib:spring:oas-stub-storage-autoconfigure-api"))
    api(project(":lib:storages:oas-stub-mongodb-storage"))
    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.kotlin.stdlib)
    implementation(libs.jackson.databind.core)
    implementation(libs.spring.boot.starter.mongodb)
    compileOnly(libs.mongodb.driver.sync)

    kapt(libs.spring.boot.configuration.processor)
}
