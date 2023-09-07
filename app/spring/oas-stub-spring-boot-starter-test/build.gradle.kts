plugins {
    `java-library`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

group = "$group.spring"
description = "OAS stub for testing"

dependencies {
    api(project(":app:spring:oas-stub-spring-boot-starter-web"))
    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.spring.boot.test.autoconfigure)
    implementation(libs.spring.boot.starter.test)
    implementation(libs.annotation.api)

    testImplementation(enforcedPlatform(libs.cucumber.bom))
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-suite")
//    testImplementation("io.cucumber:cucumber-java")
//    testImplementation("io.cucumber:cucumber-junit-platform-engine")
//    testImplementation("io.cucumber:cucumber-spring")
    testImplementation(libs.spring.web)
    testImplementation(libs.rest.assured) {
        exclude(group = "org.apache.groovy")
    }
    testImplementation(libs.groovy.xml)
    testImplementation(libs.groovy.json)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
