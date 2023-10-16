plugins {
    `java-library`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

description = "OAS stub engine"

dependencies {
    api(project(":lib:oas-stub-model"))
    api(project(":lib:oas-stub-plugin"))
    api(project(":lib:oas-stub-storage-api"))
    api(libs.servlet.api)

    implementation(libs.kotlin.stdlib)
    implementation(libs.inject.api)
    implementation(libs.ws.rs.api)
    implementation(libs.jersey.client)
    implementation(libs.mail.api)
    implementation(libs.validation.api)
    implementation(libs.swagger.core.annotations.jakarta)
    implementation(libs.swagger.perser) {
        exclude(group = "io.swagger.core.v3", module = "swagger-core")
        exclude(group = "com.google.guava")
    }
    implementation(libs.guava)
    implementation(libs.swagger.core.jakarta)
    implementation(libs.caffeine)
    implementation(libs.groovy.core)
    implementation(libs.slf4j.api)
    implementation("io.github.ktakashi.peg:parser-combinators:1.0.1")

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
