package io.github.ktakashi.oas.storages.inmemory

import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.storage.apis.PersistentStorage
import io.github.ktakashi.oas.storage.apis.SessionStorage
import java.time.Duration
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

class InMemorySessionStorage: SessionStorage {
    private val storage = ConcurrentHashMap<String, Any>()
    override fun <T> put(key: String, value: T, ttl: Duration) {
        storage[key] = value as Any
    }

    override fun <T: Any> get(key: String, type: Class<T>): Optional<T> = storage[key]?.let { v ->
        if (type.isAssignableFrom(v.javaClass)) {
            Optional.of(v as T)
        } else {
            Optional.empty()
        }
    } ?: Optional.empty()

    override fun delete(key: String): Boolean = storage.remove(key) != null
}

class InMemoryPersistentStorage: PersistentStorage {
    private val storage = ConcurrentHashMap<String, ApiDefinitions>()
    override fun getApiDefinition(applicationName: String): Optional<ApiDefinitions> = Optional.ofNullable(storage[applicationName])

    override fun setApiDefinition(applicationName: String, apiDefinitions: ApiDefinitions): Boolean {
        storage[applicationName] = apiDefinitions
        return true
    }

    override fun deleteApiDefinition(name: String): Boolean {
        storage.remove(name)
        return true
    }

}
