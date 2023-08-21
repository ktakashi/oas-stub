package io.github.ktakashi.oas.storages.hazelcast.configurations

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import com.hazelcast.client.config.ClientFailoverConfig
import com.hazelcast.config.SerializerConfig
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.nio.serialization.Serializer
import com.hazelcast.security.UsernamePasswordCredentials
import io.github.ktakashi.oas.storage.apis.PersistentStorage
import io.github.ktakashi.oas.storage.apis.SessionStorage
import io.github.ktakashi.oas.storages.hazelcast.HazelcastStorage
import io.github.ktakashi.oas.storages.hazelcast.JsonSerializer
import java.util.Optional
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

@AutoConfiguration
@ConditionalOnMissingBean(value = [HazelcastInstance::class])
@ConditionalOnExpression("'hazelcast'.equals('\${oas.storage.type.session}') or 'hazelcast'.equals('\${oas.storage.type.persistent}') ")
@EnableConfigurationProperties(HazelcastStorageProperties::class)
class AutoHazelcastConfiguration(private val properties: HazelcastStorageProperties) {
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    fun hazelcastInstance(objectMapper: ObjectMapper): HazelcastInstance {
        val instance = properties.instance
        val config = instance?.let(this::initClientConfig) ?: ClientConfig()
        val serializer = JsonSerializer(objectMapper, properties.typeId)
        addSerializer(config, serializer)
        return Optional.ofNullable(instance)
                .map<HazelcastInstanceProperties>(HazelcastMainInstanceProperties::failover)
                .map(this::initClientConfig)
                .map { c -> addSerializer(c, serializer) }
                .map { fo -> ClientFailoverConfig().addClientConfig(config).addClientConfig(fo) }
                .map(HazelcastClient::newHazelcastFailoverClient)
                .orElseGet { HazelcastClient.newHazelcastClient(config) }
    }

    private fun addSerializer(config: ClientConfig, serializer: Serializer) = config.apply {
        serializationConfig.addSerializerConfig(SerializerConfig().apply {
            implementation = serializer
            typeClass = JsonNode::class.java
        })
    }
    private fun initClientConfig(instance: HazelcastInstanceProperties) = ClientConfig().apply {
        setCredentials(UsernamePasswordCredentials(instance.username, instance.password))
        instanceName = instance.name
        clusterName = instance.clusterName
        networkConfig.addresses = instance.nodeIps?.toList() ?: listOf()
        networkConfig.isSmartRouting = true
    }
}

@AutoConfiguration(
        beforeName = ["io.github.ktakashi.oas.storages.inmemory.configurations.AutoInMemorySessionStorageConfiguration"],
        after = [AutoHazelcastConfiguration::class]
)
@Configuration
@ConditionalOnBean(value = [HazelcastInstance::class])
@ConditionalOnProperty(name = [ "oas.storage.type.session" ], havingValue = "hazelcast")
@EnableConfigurationProperties(HazelcastStorageProperties::class)
class AutoHazelcastSessionStorageConfiguration(private val properties: HazelcastStorageProperties) {
    @Bean
    @ConditionalOnMissingBean
    fun sessionStorage(hazelcastInstance: HazelcastInstance, objectMapper: ObjectMapper): SessionStorage =
        HazelcastStorage(objectMapper, hazelcastInstance, properties.sessionMap)
}

@AutoConfiguration(
        beforeName = ["io.github.ktakashi.oas.storages.inmemory.configurations.AutoInMemoryPersistentStorageConfiguration"],
        after = [AutoHazelcastConfiguration::class]
)
@Configuration
@ConditionalOnBean(value = [HazelcastInstance::class])
@ConditionalOnProperty(name = [ "oas.storage.type.persistent" ], havingValue = "hazelcast")
@EnableConfigurationProperties(HazelcastStorageProperties::class)
class AutoHazelcastPersistentStorageConfiguration(private val properties: HazelcastStorageProperties) {
    @Bean
    @ConditionalOnMissingBean
    fun persistentStorage(hazelcastInstance: HazelcastInstance, objectMapper: ObjectMapper): PersistentStorage =
            HazelcastStorage(objectMapper, hazelcastInstance, properties.sessionMap)
}


@ConfigurationProperties(prefix = "oas.storage.hazelcast")
data class HazelcastStorageProperties(var sessionMap: String = "sessionMap",
                                      var persistentMap: String = "persistentMap",
                                      var typeId: Int = Int.MAX_VALUE,
                                      @NestedConfigurationProperty var instance: HazelcastMainInstanceProperties?)


sealed interface HazelcastInstanceProperties {
    val name: String?
    val username: String?
    val password: String?
    val clusterName: String?
    val nodeIps: Array<String>?

    fun areEqual(other: HazelcastInstanceProperties): Boolean {
        if (name != other.name) return false
        if (username != other.username) return false
        if (password != other.password) return false
        if (clusterName != other.clusterName) return false
        if (nodeIps != null) {
            if (other.nodeIps == null) return false
            if (!nodeIps.contentEquals(other.nodeIps)) return false
        } else if (other.nodeIps != null) return false

        return true
    }

    fun hash(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + (username?.hashCode() ?: 0)
        result = 31 * result + (password?.hashCode() ?: 0)
        result = 31 * result + (clusterName?.hashCode() ?: 0)
        result = 31 * result + (nodeIps?.contentHashCode() ?: 0)
        return result
    }
}
data class HazelcastFailoverInstanceProperties(override val name: String?,
                                               override val username: String?,
                                               override val password: String?,
                                               override val clusterName: String?,
                                               override val nodeIps: Array<String>?): HazelcastInstanceProperties {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return areEqual(other as HazelcastInstanceProperties)
    }

    override fun hashCode(): Int = hash()

    override fun toString(): String {
        return "HazelcastFailoverInstanceProperties(name=$name, username=$username, clusterName=$clusterName, nodeIps=${nodeIps?.contentToString()})"
    }
}

data class HazelcastMainInstanceProperties(override val name: String?,
                                           override val username: String?,
                                           override val password: String?,
                                           override val clusterName: String?,
                                           override val nodeIps: Array<String>?,
                                           val failover: HazelcastFailoverInstanceProperties?): HazelcastInstanceProperties {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HazelcastMainInstanceProperties
        return areEqual(other) && failover == other.failover
    }

    override fun hashCode(): Int {
        var result = hash()
        result = 31 * result + (failover?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "HazelcastMainInstanceProperties(name=$name, username=$username, clusterName=$clusterName, nodeIps=${nodeIps?.contentToString()}, failover=$failover)"
    }
}
