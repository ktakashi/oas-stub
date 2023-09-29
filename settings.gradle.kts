plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
    id("com.gradle.enterprise") version "3.14.1"
}

gradleEnterprise {
    if (System.getenv("CI") != null) {
        buildScan {
            publishAlways()
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}

rootProject.name = "oas-stub"

include(":lib:oas-stub-model")
include(":lib:oas-stub-storage-api")
include(":lib:storages:oas-stub-inmemory-storage")
include(":lib:storages:oas-stub-hazelcast-storage")
include(":lib:storages:oas-stub-mongodb-storage")
include(":lib:oas-stub-plugin")
include(":lib:oas-stub-engine")
include(":lib:oas-stub-web")

include(":app:jersey:oas-stub-jersey-resource")

include(":app:guice:oas-stub-storage-guice-module-api")
include(":app:guice:oas-stub-guice-module")
include(":app:guice:oas-stub-hazelcast-storage-guice-module")


include(":app:spring:oas-stub-storage-autoconfigure-api")
include(":app:spring:oas-stub-inmemory-storage-autoconfigure")
include(":app:spring:oas-stub-mongodb-storage-autoconfigure")
include(":app:spring:oas-stub-hazelcast-storage-autoconfigure")
include(":app:spring:oas-stub-spring-boot-starter-web")
include(":app:spring:oas-stub-spring-test-api")
include(":app:spring:oas-stub-spring-boot-starter-test")
include(":app:spring:oas-stub-spring-stub-server")
include(":app:spring:oas-stub-spring")

include(":test:cucumber-test")