plugins {
    alias(libs.plugins.kotlin.spring)
}

description = "OAS stub Spring Boot application"
val springBootVersion by extra(property("spring-boot.version") as String)

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:${springBootVersion}")
    }
}

dependencies {
    implementation(project(":lib:oas-stub-engine"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("jakarta.servlet:jakarta.servlet-api")

    testImplementation(project(":lib:storages:inmemory:oas-stub-inmemory-storage-starter"))
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
