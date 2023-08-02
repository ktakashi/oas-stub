plugins {
    `java-library`
    `maven-publish`
    signing
}

group = "$group.engine"
description = "OAS stub engine"

dependencies {
    implementation(project(":lib:oas-stub-plugin"))
    implementation(project(":lib:storages:oas-stub-storage-api"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("jakarta.servlet:jakarta.servlet-api")
    implementation("jakarta.inject:jakarta.inject-api")
    implementation("io.swagger.parser.v3:swagger-parser:2.1.16")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.7")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
