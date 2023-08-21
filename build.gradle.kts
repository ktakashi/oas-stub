plugins {
    `java-library`
    `maven-publish`
    id(libs.plugins.kotlin.jvm.get().pluginId)
    id(libs.plugins.dokka.get().pluginId)
    id("io.spring.dependency-management") version "1.1.2"
}

val kotlinVersion = libs.versions.kotlin.get()
val springBootVersion = libs.versions.spring.boot.get()
val mongodbVersion = libs.versions.mongodb.get()
val hazelcastVersion = libs.versions.hazelcast.get()

val dokkaPlugin = libs.plugins.dokka.get().pluginId

val servletApiVersion by extra("6.0.0")
val swaggerCoreVersion by extra("2.2.15")
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
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")
    apply(plugin = dokkaPlugin)
    apply(plugin = "io.spring.dependency-management")

    ext["mongodb.version"] = mongodbVersion
    ext["hazelcast.version"] = hazelcastVersion
    ext["spring-boot.version"] = springBootVersion

    dependencyManagement {
        dependencies {
            dependency("jakarta.servlet:jakarta.servlet-api:${servletApiVersion}")
            dependency("jakarta.inject:jakarta.inject-api:2.0.1")
            dependency("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
            dependency("jakarta.mail:jakarta.mail-api:2.1.2")
            dependency("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
            dependency("jakarta.validation:jakarta.validation-api:3.0.2")
            dependency("io.swagger.parser.v3:swagger-parser:2.1.16")
            dependency("io.swagger.core.v3:swagger-core-jakarta:${swaggerCoreVersion}")
            dependency("io.swagger.core.v3:swagger-annotations:${swaggerCoreVersion}")
            dependency("io.swagger.core.v3:swagger-annotations-jakarta:${swaggerCoreVersion}")

            dependency("com.github.ben-manes.caffeine:caffeine:3.1.7")
            dependency("org.apache.groovy:groovy:4.0.13")
            dependency("io.rest-assured:rest-assured:5.3.1")
            dependency("org.slf4j:slf4j-api:2.0.7")
            dependency("org.awaitility:awaitility:4.2.0")
        }
        imports {
            mavenBom("org.jetbrains.kotlin:kotlin-bom:${kotlinVersion}")
            mavenBom("org.junit:junit-bom:${junitVersion}")
            mavenBom("com.fasterxml.jackson:jackson-bom:2.15.2")
            mavenBom("io.cucumber:cucumber-bom:7.13.0")
        }
    }
}
