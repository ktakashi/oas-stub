package io.github.ktakashi.oas.storages.mongodb.configurations

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.client.MongoClient
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import io.github.ktakashi.oas.storages.apis.conditions.OAS_STUB_INMEMORY_PERSISTENT_STORAGE_MODULE
import io.github.ktakashi.oas.storages.apis.conditions.OAS_STUB_INMEMORY_SESSION_STORAGE_MODULE
import io.github.ktakashi.oas.storages.apis.conditions.OAS_STUB_STORAGE
import io.github.ktakashi.oas.storages.apis.conditions.OAS_STUB_STORAGE_TYPE_PERSISTENT
import io.github.ktakashi.oas.storages.apis.conditions.OAS_STUB_STORAGE_TYPE_SESSION
import io.github.ktakashi.oas.storages.mongodb.MongodbPersistentStorage
import io.github.ktakashi.oas.storages.mongodb.MongodbSessionStorage
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@AutoConfiguration(
        beforeName = [OAS_STUB_INMEMORY_SESSION_STORAGE_MODULE],
        after = [MongoAutoConfiguration::class]
)
@Configuration
@ConditionalOnBean(value = [MongoClient::class])
@ConditionalOnProperty(name = [ OAS_STUB_STORAGE_TYPE_SESSION ], havingValue = "mongodb")
@EnableConfigurationProperties(MongodbStorageProperties::class)
class AutoMongodbSessionStorageConfiguration(private val properties: MongodbStorageProperties) {
    @Bean
    @ConditionalOnMissingBean(SessionStorage::class)
    fun sessionStorage(mongoClient: MongoClient, objectMapper: ObjectMapper): SessionStorage {
        val session = properties.session ?: throw IllegalStateException("'oas.storage.mongodb.session' must be provided")
        return MongodbSessionStorage(objectMapper, mongoClient, session.database, session.collection)
    }
}

@AutoConfiguration(
        beforeName = [OAS_STUB_INMEMORY_PERSISTENT_STORAGE_MODULE],
        after = [MongoAutoConfiguration::class]
)
@Configuration
@ConditionalOnBean(value = [MongoClient::class])
@ConditionalOnProperty(name = [ OAS_STUB_STORAGE_TYPE_PERSISTENT ], havingValue = "mongodb")
@EnableConfigurationProperties(MongodbStorageProperties::class)
class AutoMongodbPersistentStorageConfiguration(private val properties: MongodbStorageProperties) {
    @Bean
    @ConditionalOnMissingBean(PersistentStorage::class)
    fun persistentStorage(mongoClient: MongoClient, objectMapper: ObjectMapper): PersistentStorage {
        val persistent = properties.persistent?: throw IllegalStateException("'oas.storage.mongodb.persistent' must be provided")
        return MongodbPersistentStorage(objectMapper, mongoClient, persistent.database, persistent.collection)
    }
}


@ConfigurationProperties(prefix = "${OAS_STUB_STORAGE}.mongodb")
data class MongodbStorageProperties(@NestedConfigurationProperty var session: MongodbConnectionProperties?,
                                    @NestedConfigurationProperty var persistent: MongodbConnectionProperties?)

data class MongodbConnectionProperties(var database: String,
                                       var collection: String)
