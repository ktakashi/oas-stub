plugins {
    `java-library`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

group = "$group.spring"
description = "OAS stub hazelcast storage starter"

dependencies {
    api(project(":lib:storages:oas-stub-hazelcast-storage"))
    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.kotlin.stdlib)
    implementation(libs.jackson.databind)

    annotationProcessor(libs.spring.boot.configuration.processor)
}
