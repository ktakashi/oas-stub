package io.github.ktakashi.oas.storages.mongodb.configurations

import com.fasterxml.jackson.databind.ObjectMapper
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import io.github.ktakashi.oas.storage.apis.PersistentStorage
import io.github.ktakashi.oas.storage.apis.SessionStorage
import io.github.ktakashi.oas.storages.mongodb.MongodbPersistentStorage
import io.github.ktakashi.oas.storages.mongodb.MongodbSessionStorage
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.util.UriComponentsBuilder

@AutoConfiguration
@ConditionalOnExpression("'mongodb'.equals('\${oas.storage.type.session}') or 'mongodb'.equals('\${oas.storage.type.persistent}') ")
@EnableConfigurationProperties(MongodbStorageProperties::class)
class AutoMongodbConfiguration(private val properties: MongodbStorageProperties) {

    @Bean
    @ConditionalOnMissingBean(MongoClient::class)
    fun mongoClient(objectMapper: ObjectMapper): MongoClient {
        val connection = properties.client ?: throw IllegalStateException("'oas.storage.mongodb.client' is required")
        val connectionString = connection.toConnectionString()
        return MongoClients.create(MongoClientSettings.builder()
                .retryWrites(connection.retryWrites)
                .retryReads(connection.retryReads)
                .applyConnectionString(connectionString)
                .build())
    }
}

@AutoConfiguration(
        beforeName = ["io.github.ktakashi.oas.storages.inmemory.configurations.AutoInMemorySessionStorageConfiguration"],
        after = [AutoMongodbConfiguration::class]
)
@Configuration
@ConditionalOnBean(value = [MongoClient::class])
@ConditionalOnProperty(name = [ "oas.storage.type.session" ], havingValue = "mongodb")
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
        beforeName = ["io.github.ktakashi.oas.storages.inmemory.configurations.AutoInMemoryPersistentStorageConfiguration"],
        after = [AutoMongodbConfiguration::class]
)
@Configuration
@ConditionalOnBean(value = [MongoClient::class])
@ConditionalOnProperty(name = [ "oas.storage.type.persistent" ], havingValue = "mongodb")
@EnableConfigurationProperties(MongodbStorageProperties::class)
class AutoMongodbPersistentStorageConfiguration(private val properties: MongodbStorageProperties) {
    @Bean
    @ConditionalOnMissingBean(PersistentStorage::class)
    fun persistentStorage(mongoClient: MongoClient, objectMapper: ObjectMapper): PersistentStorage {
        val persistent = properties.persistent?: throw IllegalStateException("'oas.storage.mongodb.persistent' must be provided")
        return MongodbPersistentStorage(objectMapper, mongoClient, persistent.database, persistent.collection)
    }
}


@ConfigurationProperties(prefix = "oas.storage.mongodb")
data class MongodbStorageProperties(@NestedConfigurationProperty var session: MongodbConnectionProperties?,
                                    @NestedConfigurationProperty var persistent: MongodbConnectionProperties?,
                                    @NestedConfigurationProperty var client: MongoClientProperties?)

data class MongodbConnectionProperties(var database: String,
                                       var collection: String)

data class MongoClientProperties(var uri: String? = null,
                                 var host: String? = null,
                                 var port: Int? = 27017,
                                 var username: String? = null,
                                 var password: String? = null,
                                 var retryWrites: Boolean = true,
                                 var retryReads: Boolean = true,
                                 var ssl: Boolean = false) {
    fun toConnectionString(): ConnectionString = if (uri.isNullOrBlank()) {
        ConnectionString(UriComponentsBuilder.newInstance()
                .scheme("mongodb")
                .userInfo(toUserInfo())
                .host(host)
                .port(port?: 27017)
                .build().toUriString())
    } else {
        ConnectionString(uri!!)
    }

    private fun toUserInfo(): String? = if (username.isNullOrBlank()) {
        null
    } else {
        if (password.isNullOrBlank()) {
            username
        } else {
            "$username:$password"
        }
    }
}
