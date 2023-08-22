plugins {
    `java-library`
    `maven-publish`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.dokka.get().pluginId)
    id("io.github.ktakashi.oas.conventions")
}
group = "$group.storage"
description = "OAS stub storage APIs"

dependencies {
    implementation(project(":lib:oas-stub-model"))
    implementation(project(":lib:oas-stub-plugin"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}
