plugins {
    `java-library`
    kotlin("kapt")
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

group = "$group.spring"
description = "OAS stub Guice server for test"

dependencies {
    api(project(":app:guice:oas-stub-guice-module"))
    api(project(":app:spring:oas-stub-spring-test-api"))
    api(libs.spring.boot.starter.core)
    implementation(libs.spring.boot.test.autoconfigure)
    implementation(libs.kotlin.stdlib)
    implementation(libs.annotation.api)
    implementation(libs.spring.test)

    kapt(libs.spring.boot.configuration.processor)

    testImplementation(enforcedPlatform(libs.cucumber.bom))
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-suite")
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.web)
    testImplementation(libs.rest.assured) {
        exclude(group = "org.apache.groovy")
    }
    testImplementation(libs.groovy.core)
    testImplementation(libs.groovy.json)
    testImplementation(libs.groovy.xml)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.bcprov.jdk18on)
    testImplementation(libs.bcpkix.jdk18on)
}
