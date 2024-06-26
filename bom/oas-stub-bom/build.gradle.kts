plugins {
    `java-platform`
    `maven-publish`
    signing // manually apply
    id("io.github.ktakashi.oas.conventions")
}

description = "OAS stub BOM"

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api(project(":lib:oas-stub-api"))
        api(project(":lib:oas-stub-model"))
        api(project(":lib:oas-stub-storage-api"))
        api(project(":lib:storages:oas-stub-inmemory-storage"))
        api(project(":lib:storages:oas-stub-hazelcast-storage"))
        api(project(":lib:storages:oas-stub-mongodb-storage"))
        api(project(":lib:oas-stub-engine"))
        api(project(":lib:oas-stub-server"))
        api(project(":lib:spring:oas-stub-storage-autoconfigure-api"))
        api(project(":lib:spring:oas-stub-inmemory-storage-autoconfigure"))
        api(project(":lib:spring:oas-stub-mongodb-storage-autoconfigure"))
        api(project(":lib:spring:oas-stub-hazelcast-storage-autoconfigure"))
        api(project(":lib:spring:oas-stub-spring-boot-starter-test"))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "oas-stub-bom"
            from(components["javaPlatform"])
        }
    }
}