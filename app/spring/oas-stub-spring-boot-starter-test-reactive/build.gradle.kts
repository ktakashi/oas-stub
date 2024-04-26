plugins {
    `java-library`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

group = "$group.spring"
description = "OAS stub for testing reactive"

dependencies {
    api(project(":app:spring:oas-stub-spring-boot-starter-web-reactive"))
    api(project(":app:spring:oas-stub-spring-test-api"))
    api(libs.spring.boot.starter.test)
    api(libs.bcprov.jdk18on)
    api(libs.bcpkix.jdk18on)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.autoconfigure)
    implementation(libs.spring.boot.test.autoconfigure)
    implementation(libs.annotation.api)
    implementation(libs.reactor.netty)
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
