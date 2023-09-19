plugins {
    `java-library`
     id(libs.plugins.kotlin.jvm.get().pluginId)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

group = "$group.guice"
description = "OAS stub Hazalcast storage Guice modules"

dependencies {
    api(project(":app:guice:oas-stub-storage-guice-module-api"))
    api(project(":lib:storages:oas-stub-hazelcast-storage"))
    implementation(libs.guice.core)
    implementation(libs.kotlin.stdlib)
    implementation(libs.jackson.databind.core)
    compileOnly(libs.hazelcast)
}
