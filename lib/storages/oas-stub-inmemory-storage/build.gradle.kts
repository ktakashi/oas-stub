plugins {
    `java-library`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

description = "OAS stub in-memory storage"

dependencies {
    implementation(platform(libs.kotlin.bom))
    api(project(":lib:oas-stub-storage-api"))
    implementation(libs.inject.api)
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}
