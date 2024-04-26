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
        api(project(":lib:oas-stub-model"))
        api(project(":lib:oas-stub-storage-api"))
        api(project(":lib:storages:oas-stub-inmemory-storage"))
        api(project(":lib:storages:oas-stub-hazelcast-storage"))
        api(project(":lib:storages:oas-stub-mongodb-storage"))
        api(project(":lib:oas-stub-plugin"))
        api(project(":lib:oas-stub-engine"))
        api(project(":lib:oas-stub-web"))
        api(project(":lib:oas-stub-web-reactive"))
        api(project(":app:jersey:oas-stub-jersey-resource"))
        api(project(":app:guice:oas-stub-storage-guice-module-api"))
        api(project(":app:guice:oas-stub-guice-module"))
        api(project(":app:guice:oas-stub-hazelcast-storage-guice-module"))
        api(project(":app:spring:oas-stub-storage-autoconfigure-api"))
        api(project(":app:spring:oas-stub-inmemory-storage-autoconfigure"))
        api(project(":app:spring:oas-stub-mongodb-storage-autoconfigure"))
        api(project(":app:spring:oas-stub-hazelcast-storage-autoconfigure"))
        api(project(":app:spring:oas-stub-spring-boot-starter-web"))
        api(project(":app:spring:oas-stub-spring-boot-starter-web-reactive"))
        api(project(":app:spring:oas-stub-spring-test-api"))
        api(project(":app:spring:oas-stub-spring-boot-starter-test"))
        api(project(":app:spring:oas-stub-spring-boot-starter-test-reactive"))
        api(project(":app:spring:oas-stub-spring-stub-server"))
        api(project(":app:spring:oas-stub-spring"))
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