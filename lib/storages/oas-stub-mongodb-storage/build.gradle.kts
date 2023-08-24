plugins {
    `java-library`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

description = "OAS stub mongodb storage"

dependencies {
    api(project(":lib:oas-stub-storage-api"))
    api(libs.mongodb.driver.sync)
    implementation(libs.jackson.databind)
    implementation(libs.kotlin.stdlib)
}
