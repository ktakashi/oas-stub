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
include(":lib:storages:oas-stub-storage-api")
include(":lib:storages:inmemory:oas-stub-inmemory-storage")
include(":lib:storages:inmemory:oas-stub-inmemory-storage-autoconfigure")
include(":lib:storages:hazelcast:oas-stub-hazelcast-storage")
include(":lib:storages:hazelcast:oas-stub-hazelcast-storage-autoconfigure")
include(":lib:storages:mongodb:oas-stub-mongodb-storage")
include(":lib:storages:mongodb:oas-stub-mongodb-storage-autoconfigure")
include(":lib:oas-stub-plugin")
include(":lib:oas-stub-engine")
include(":app:oas-stub-spring")
