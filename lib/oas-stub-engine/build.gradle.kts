plugins {
    `java-library`
    `maven-publish`
    signing
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.dokka.get().pluginId)
    id("io.github.ktakashi.oas.conventions")
}

group = "$group.engine"
description = "OAS stub engine"

dependencies {
    implementation(enforcedPlatform(libs.kotlin.bom))
    api(project(":lib:oas-stub-model"))
    api(project(":lib:oas-stub-plugin"))
    api(project(":lib:storages:oas-stub-storage-api"))
    api("org.jetbrains.kotlin:kotlin-stdlib")
    api(libs.servlet.api)
    implementation(libs.inject.api)
    implementation(libs.ws.rs.api)
    implementation(libs.jersey.client)
    implementation(libs.mail.api)
    implementation(libs.validation.api)
    implementation(libs.swagger.core.annotations.jakarta)
    implementation(libs.swagger.perser) {
        exclude(group = "io.swagger.core.v3", module = "swagger-core")
    }
    implementation(libs.swagger.core.jakarta)
    implementation(libs.caffeine)
    implementation(libs.groovy)
    implementation(libs.slf4j.api)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
