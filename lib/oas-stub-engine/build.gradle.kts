plugins {
    `java-library`
    `maven-publish`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.dokka.get().pluginId)
    id("io.github.ktakashi.oas.conventions")
}

group = "$group.engine"
description = "OAS stub engine"

dependencies {
    api(project(":lib:oas-stub-model"))
    api(project(":lib:oas-stub-plugin"))
    api(project(":lib:storages:oas-stub-storage-api"))
    api("org.jetbrains.kotlin:kotlin-stdlib")
    api("jakarta.servlet:jakarta.servlet-api")
    implementation("jakarta.inject:jakarta.inject-api")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api")
    implementation("org.glassfish.jersey.core:jersey-client:3.1.3")
    implementation("jakarta.mail:jakarta.mail-api")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("io.swagger.core.v3:swagger-annotations-jakarta")
    implementation("io.swagger.parser.v3:swagger-parser") {
        exclude(group = "io.swagger.core.v3", module = "swagger-core")
    }
    implementation("io.swagger.core.v3:swagger-core-jakarta")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.apache.groovy:groovy")
    implementation("org.slf4j:slf4j-api")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
