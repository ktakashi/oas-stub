plugins {
    `java-library`
    `maven-publish`
    signing
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.dokka.get().pluginId)
    id("io.github.ktakashi.oas.conventions")
}

group = "$group.storage"
description = "OAS stub in-memory storage"

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(project(":lib:oas-stub-model"))
    implementation(project(":lib:oas-stub-plugin"))
    implementation(project(":lib:storages:oas-stub-storage-api"))
    implementation(libs.inject.api)
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}
