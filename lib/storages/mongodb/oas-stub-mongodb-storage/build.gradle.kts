plugins {
    `java-library`
    `maven-publish`
    signing
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.dokka.get().pluginId)
    id("io.github.ktakashi.oas.conventions")
}

group = "$group.storage"
description = "OAS stub mongodb storage"

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.jackson.bom))
    implementation(project(":lib:oas-stub-model"))
    implementation(project(":lib:oas-stub-plugin"))
    implementation(project(":lib:storages:oas-stub-storage-api"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    api(libs.mongodb.driver.sync)
}
