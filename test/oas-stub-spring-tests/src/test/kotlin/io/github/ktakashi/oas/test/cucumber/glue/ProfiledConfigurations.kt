package io.github.ktakashi.oas.test.cucumber.glue

import io.github.ktakashi.oas.storages.hazelcast.configurations.AutoHazelcastPersistentStorageConfiguration
import io.github.ktakashi.oas.storages.hazelcast.configurations.AutoHazelcastSessionStorageConfiguration
import io.github.ktakashi.oas.storages.mongodb.configurations.AutoMongodbPersistentStorageConfiguration
import io.github.ktakashi.oas.storages.mongodb.configurations.AutoMongodbSessionStorageConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("hazelcast")
@EnableAutoConfiguration(exclude = [
    AutoMongodbSessionStorageConfiguration::class,
    AutoMongodbPersistentStorageConfiguration::class,
    MongoAutoConfiguration::class
])
class HazelcastProfiledConfigurations

@Configuration
@Profile("mongodb")
@EnableAutoConfiguration(exclude = [
    AutoHazelcastSessionStorageConfiguration::class,
    AutoHazelcastPersistentStorageConfiguration::class
])
class MongodbProfiledConfigurations

@Configuration
@Profile("hazeldb")
@EnableAutoConfiguration(exclude = [
    AutoMongodbSessionStorageConfiguration::class,
    AutoHazelcastPersistentStorageConfiguration::class
])
class HazeldbProfiledConfigurations