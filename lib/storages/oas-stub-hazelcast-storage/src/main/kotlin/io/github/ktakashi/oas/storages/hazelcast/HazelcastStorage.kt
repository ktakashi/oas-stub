package io.github.ktakashi.oas.storages.hazelcast

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.nio.serialization.ByteArraySerializer
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import java.time.Duration
import java.util.Optional
import java.util.concurrent.TimeUnit

class HazelcastStorage(private val objectMapper: ObjectMapper,
                       hazelcastInstance: HazelcastInstance,
                       mapName: String) {
    private val map = hazelcastInstance.getMap<String, JsonNode>(mapName)
    fun <T> put(key: String, value: T, ttl: Duration): Boolean {
        val node = objectMapper.valueToTree<JsonNode>(value)
        map.put(key, node)
        if (!ttl.isZero) {
            map.setTtl(key, ttl.toMillis(), TimeUnit.MILLISECONDS)
        }
        return true
    }

    fun <T : Any> get(key: String, type: Class<T>): Optional<T> = Optional.ofNullable(map[key])
            .map { v -> objectMapper.treeToValue(v, type) }

    fun delete(key: String): Boolean = map.remove(key) != null
}

class HazelcastSessionStorage(private val hazelcastStorage: HazelcastStorage): SessionStorage {
    override fun <T> put(key: String, value: T, ttl: Duration): Boolean = hazelcastStorage.put(key, value, ttl)

    override fun <T : Any> get(key: String, type: Class<T>): Optional<T> = hazelcastStorage.get(key, type)

    override fun delete(key: String): Boolean = hazelcastStorage.delete(key)
}

class HazelcastPersistentStorage(private val hazelcastStorage: HazelcastStorage): PersistentStorage {
    override fun getApiDefinition(applicationName: String): Optional<ApiDefinitions> = hazelcastStorage.get(applicationName, ApiDefinitions::class.java)

    override fun setApiDefinition(applicationName: String, apiDefinitions: ApiDefinitions): Boolean = hazelcastStorage.put(applicationName, apiDefinitions, Duration.ZERO)

    override fun deleteApiDefinition(name: String): Boolean = hazelcastStorage.delete(name)

}

class JsonSerializer(private val objectMapper: ObjectMapper,
                     private val typeId: Int): ByteArraySerializer<JsonNode> {
    override fun getTypeId(): Int = typeId

    override fun read(buffer: ByteArray): JsonNode = objectMapper.readTree(buffer)

    override fun write(`object`: JsonNode?): ByteArray = objectMapper.writeValueAsBytes(`object`)

}
