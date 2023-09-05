plugins {
    `java-library`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

description = "OAS stub hazelcast storage"

dependencies {
    api(project(":lib:oas-stub-storage-api"))
    api(libs.hazelcast)

    implementation(libs.jackson.databind)
    implementation(libs.kotlin.stdlib)
}