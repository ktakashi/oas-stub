package io.github.ktakashi.oas.storages.mongodb.configurations

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
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper

private const val OAS_STUB_STORAGE_MONGODB = "${OAS_STUB_STORAGE}.mongodb"
@AutoConfiguration(
        beforeName = [OAS_STUB_INMEMORY_SESSION_STORAGE_MODULE],
        after = [MongoAutoConfiguration::class]
)
@Configuration
@ConditionalOnClass(MongoClient::class)
@ConditionalOnBean(value = [MongoClient::class])
@ConditionalOnProperty(name = [ OAS_STUB_STORAGE_TYPE_SESSION ], havingValue = "mongodb")
@EnableConfigurationProperties(MongodbStorageProperties::class)
class AutoMongodbSessionStorageConfiguration(private val properties: MongodbStorageProperties) {
    @Bean
    @ConditionalOnMissingBean(SessionStorage::class)
    fun sessionStorage(mongoClient: MongoClient, jsonMapper: JsonMapper): SessionStorage {
        val session = properties.session ?: throw IllegalStateException("'${OAS_STUB_STORAGE_MONGODB}.session' must be provided")
        return MongodbSessionStorage(jsonMapper, mongoClient, session.database, session.collection)
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
    fun persistentStorage(mongoClient: MongoClient, jsonMapper: JsonMapper): PersistentStorage {
        val persistent = properties.persistent?: throw IllegalStateException("'${OAS_STUB_STORAGE_MONGODB}.persistent' must be provided")
        return MongodbPersistentStorage(jsonMapper, mongoClient, persistent.database, persistent.collection)
    }
}


@ConfigurationProperties(prefix = OAS_STUB_STORAGE_MONGODB)
data class MongodbStorageProperties(
    /**
     * Session DB connection properties
     */
    @NestedConfigurationProperty var session: MongodbConnectionProperties?,
    /**
     * Persistent DB connection properties
     */
    @NestedConfigurationProperty var persistent: MongodbConnectionProperties?)

data class MongodbConnectionProperties(
    /**
     * Mongo database name
     */
    var database: String,
    /**
     * Mongo collection name
     */
    var collection: String
)
