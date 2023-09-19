plugins {
    `java-library`
     id(libs.plugins.kotlin.jvm.get().pluginId)
    alias(libs.plugins.kotlin.spring)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

group = "$group.guice"
description = "OAS stub Guice modules"

dependencies {
    implementation(project(":app:jersey:oas-stub-jersey-resource"))
    implementation(project(":app:guice:oas-stub-storage-guice-module-api"))
    implementation(project(":lib:oas-stub-engine"))
    implementation(project(":lib:oas-stub-web"))
    implementation(project(":lib:storages:oas-stub-inmemory-storage"))

    implementation(libs.guice.core)
    implementation(libs.guice.servlet)
    implementation(libs.guice.bridge)
    implementation(libs.jetty.server)
    implementation(libs.jetty.ee10.webapp)
    implementation(libs.jersey.server)
    implementation(libs.jersey.container.servlet)
    implementation(libs.jersey.inject.hk2)
    implementation(libs.jersey.media.json.jackson)
    implementation(libs.jackson.databind.core)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.rest.assured)
}
