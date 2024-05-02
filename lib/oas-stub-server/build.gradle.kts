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

    // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
    testImplementation("ch.qos.logback:logback-classic:1.5.6")

}