plugins {
    `java-library`
    `maven-publish`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.dokka.get().pluginId)
    id("io.github.ktakashi.oas.conventions")
}

group = "$group.model"
description = "OAS stub model"

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-annotations")
}
