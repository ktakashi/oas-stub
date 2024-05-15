plugins {
    `java-library`
    kotlin("kapt")
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

group = "$group.spring"
description = "OAS stub for testing"

dependencies {
    api(project(":lib:oas-stub-server"))
    api(project(":lib:spring:oas-stub-inmemory-storage-autoconfigure"))
    api(project(":lib:spring:oas-stub-hazelcast-storage-autoconfigure"))
    api(project(":lib:spring:oas-stub-mongodb-storage-autoconfigure"))
    api(libs.spring.boot.starter.test)
    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.spring.boot.test.autoconfigure)
    implementation(libs.annotation.api)
    implementation(libs.kotlin.stdlib)
    implementation(libs.koin.core)
    implementation(libs.koin.jvm)

    kapt(libs.spring.boot.configuration.processor)

    testImplementation(enforcedPlatform(libs.cucumber.bom))
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-suite")
    testImplementation(libs.spring.web)
    testImplementation(libs.rest.assured) {
        exclude(group = "org.apache.groovy")
    }
    testImplementation(libs.groovy.core)
    testImplementation(libs.groovy.json)
    testImplementation(libs.groovy.xml)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
