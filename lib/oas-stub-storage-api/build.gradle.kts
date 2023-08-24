plugins {
    `java-library`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

description = "OAS stub storage APIs"

dependencies {
    api(project(":lib:oas-stub-model"))
    api(project(":lib:oas-stub-plugin"))
    implementation(libs.kotlin.stdlib)
}
