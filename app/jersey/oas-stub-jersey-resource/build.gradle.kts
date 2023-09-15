plugins {
    `java-library`
     id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

group = "$group.jersey"
description = "OAS stub Jersey config"

dependencies {
    implementation(project(":lib:oas-stub-web"))
    implementation(libs.jersey.server)
    implementation(libs.slf4j.api)
    implementation(libs.swagger.core.jaxrs2.jakarta)
}
