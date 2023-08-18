plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.4.0"
}

rootProject.name = "oas-stub"

include(":lib:oas-stub-model")
include(":lib:storages:oas-stub-storage-api")
include(":lib:storages:inmemory:oas-stub-inmemory-storage")
include(":lib:storages:inmemory:oas-stub-inmemory-storage-starter")
include(":lib:storages:hazelcast:oas-stub-hazelcast-storage")
include(":lib:storages:hazelcast:oas-stub-hazelcast-storage-starter")
include(":lib:storages:mongodb:oas-stub-mongodb-storage")
include(":lib:storages:mongodb:oas-stub-mongodb-storage-starter")
include(":lib:oas-stub-plugin")
include(":lib:oas-stub-engine")
include(":app:oas-stub-spring")
