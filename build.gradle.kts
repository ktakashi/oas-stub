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
