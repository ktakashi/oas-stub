plugins {
    `java-library`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

description = "OAS stub hazelcast storage"

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.jackson.bom))

    api(project(":lib:oas-stub-storage-api"))

    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    api(libs.hazelcast)
}
