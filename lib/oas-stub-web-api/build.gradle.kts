plugins {
    `java-library`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

description = "OAS stub web API"

dependencies {
    api(project(":lib:oas-stub-engine"))

    implementation(libs.aspectjrt)
    implementation(libs.slf4j.api)
    implementation(libs.kotlin.stdlib)
    implementation(libs.inject.api)
    implementation(libs.swagger.core.annotations.jakarta)
    implementation(libs.projectreactor.reactor.core)
    implementation(libs.jackson.module.kotlin)
}