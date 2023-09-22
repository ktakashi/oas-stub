plugins {
    `java-library`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id("io.github.ktakashi.oas.conventions")
}

group = "$group.test"
description = "OAS stub Cucumber test utilities"

dependencies {
    implementation(project(":lib:oas-stub-storage-api"))
    implementation(libs.inject.api)
    implementation(libs.spring.web)
    implementation(libs.jackson.databind.core)
    implementation(enforcedPlatform(libs.cucumber.bom))
    implementation(platform(libs.junit.bom))
    implementation("org.junit.jupiter:junit-jupiter")
    implementation("org.junit.platform:junit-platform-suite")
    implementation("io.cucumber:cucumber-java")
    implementation("io.cucumber:cucumber-junit-platform-engine")
    implementation(libs.rest.assured)
    runtimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(libs.hazelcast)
    implementation(libs.mongodb.driver.sync)
    implementation(libs.flapdoodle.embed.mongo)
}
