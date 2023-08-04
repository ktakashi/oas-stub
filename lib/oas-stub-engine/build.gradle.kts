plugins {
    id("io.github.ktakashi.oas.conventions")
}

group = "$group.engine"
description = "OAS stub engine"

dependencies {
    implementation(project(":lib:oas-stub-plugin"))
    implementation(project(":lib:storages:oas-stub-storage-api"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("jakarta.servlet:jakarta.servlet-api")
    implementation("jakarta.inject:jakarta.inject-api")
    implementation("jakarta.ws.rs:jakarta.ws.rs-api")
    implementation("jakarta.mail:jakarta.mail-api")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("io.swagger.parser.v3:swagger-parser")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.apache.groovy:groovy")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
