plugins {
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.dokka.get().pluginId)
    id("io.spring.dependency-management") version "1.1.2"
    id("io.github.ktakashi.oas.conventions")
    `java-library`
    `maven-publish`
}

val kotlinVersion = property("kotlin.version")
val servletApiVersion by extra("6.0.0")
val junitVersion by extra("5.9.3")

description = "OAS stub"

allprojects {
    group = group
    version = version

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "io.github.ktakashi.oas.conventions")
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    dependencyManagement {
        dependencies {
            dependency("jakarta.servlet:jakarta.servlet-api:${servletApiVersion}")
            dependency("jakarta.inject:jakarta.inject-api:2.0.1")
            dependency("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
            dependency("jakarta.mail:jakarta.mail-api:2.1.2")
            dependency("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
            dependency("jakarta.validation:jakarta.validation-api:3.0.2")
            dependency("io.swagger.parser.v3:swagger-parser:2.1.16")
            dependency("com.github.ben-manes.caffeine:caffeine:3.1.7")
            dependency("org.apache.groovy:groovy:4.0.13")
        }
        imports {
            mavenBom("org.jetbrains.kotlin:kotlin-bom:${kotlinVersion}")
            mavenBom("org.junit:junit-bom:${junitVersion}")
        }
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
    }
}
