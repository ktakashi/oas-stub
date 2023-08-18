package io.github.ktakashi.oas.storages.hazelcast

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.hazelcast.core.HazelcastInstance
import com.hazelcast.nio.serialization.ByteArraySerializer
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.storage.apis.PersistentStorage
import io.github.ktakashi.oas.storage.apis.SessionStorage
import java.time.Duration
import java.util.Optional
import java.util.concurrent.TimeUnit

class HazelcastStorage(private val objectMapper: ObjectMapper,
                       hazelcastInstance: HazelcastInstance,
                       mapName: String): SessionStorage, PersistentStorage {
    private val map = hazelcastInstance.getMap<String, JsonNode>(mapName)
    override fun <T> put(key: String, value: T, ttl: Duration): Boolean {
        val node = objectMapper.valueToTree<JsonNode>(value)
        map.put(key, node)
        if (!ttl.isZero) {
            map.setTtl(key, ttl.toMillis(), TimeUnit.MILLISECONDS)
        }
        return true
    }

    override fun <T : Any> get(key: String, type: Class<T>): Optional<T> = Optional.ofNullable(map[key])
            .map { v -> objectMapper.treeToValue(v, type) }

    override fun delete(key: String): Boolean = map.remove(key) != null

    override fun getApiDefinition(applicationName: String): Optional<ApiDefinitions> = get(applicationName, ApiDefinitions::class.java)

    override fun setApiDefinition(applicationName: String, apiDefinitions: ApiDefinitions): Boolean = put(applicationName, apiDefinitions)

    override fun deleteApiDefinition(name: String): Boolean = delete(name)

}

class JsonSerializer(private val objectMapper: ObjectMapper,
                     private val typeId: Int): ByteArraySerializer<JsonNode> {
    override fun getTypeId(): Int = typeId

    override fun read(buffer: ByteArray): JsonNode = objectMapper.readTree(buffer)

    override fun write(`object`: JsonNode?): ByteArray = objectMapper.writeValueAsBytes(`object`)

}
