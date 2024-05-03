plugins {
    `java-library`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id("io.github.ktakashi.oas.conventions")
    id("io.github.ktakashi.oas.deployed")
}

description = "OAS stub web Netty"

dependencies {
    api(project(":lib:oas-stub-engine"))
    implementation(project(":lib:storages:oas-stub-inmemory-storage"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.koin.core)
    implementation(libs.koin.jvm)
    implementation(libs.reactor.netty)
    implementation(libs.aspectjrt)
    implementation(libs.slf4j.api)
    implementation(libs.swagger.core.annotations.jakarta)
    implementation(libs.projectreactor.reactor.core)
    implementation(libs.jackson.module.kotlin)

    testImplementation(project(":test:cucumber-test"))
    testImplementation(libs.hazelcast)
    testImplementation(libs.mongodb.driver.sync)
    testImplementation(enforcedPlatform(libs.cucumber.bom))
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-suite")
    testImplementation("io.cucumber:cucumber-java")
    testImplementation("io.cucumber:cucumber-junit-platform-engine")
    testImplementation("io.cucumber:cucumber-spring")
    testImplementation(libs.spring.boot.starter.test)
}