plugins {
    kotlin("jvm") version "1.9.0"
    id("org.jetbrains.dokka") version "1.8.20"
    id("io.spring.dependency-management") version "1.1.2"
}

val kotlinVersion by extra("1.9.0")
val servletApiVersion by extra("6.0.0")
val junitVersion by extra("5.9.3")

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
    apply(plugin = "org.jetbrains.dokka")
    kotlin {
        jvmToolchain(17)
    }

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
