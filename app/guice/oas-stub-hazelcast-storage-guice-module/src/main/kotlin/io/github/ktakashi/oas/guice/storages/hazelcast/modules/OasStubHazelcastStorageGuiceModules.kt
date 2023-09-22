package io.github.ktakashi.oas.guice.storages.hazelcast.modules

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.AbstractModule
import com.hazelcast.core.HazelcastInstance
import io.github.ktakashi.oas.guice.storages.apis.OasStubPersistentStorageModuleCreator
import io.github.ktakashi.oas.guice.storages.apis.OasStubSessionStorageModuleCreator
import io.github.ktakashi.oas.plugin.apis.Storage
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import io.github.ktakashi.oas.storages.hazelcast.HazelcastPersistentStorage
import io.github.ktakashi.oas.storages.hazelcast.HazelcastSessionStorage
import io.github.ktakashi.oas.storages.hazelcast.HazelcastStorage

class OasStubHazelcastSessionStorageModule
    private constructor(private val hazelcastInstance: HazelcastInstance,
                        private val objectMapper: ObjectMapper,
                        private val mapName: String): AbstractModule() {
    companion object {
        @JvmStatic
        fun createModuleCreator(hazelcastInstance: HazelcastInstance, mapName: String) = OasStubSessionStorageModuleCreator { objectMapper ->
            OasStubHazelcastSessionStorageModule(hazelcastInstance, objectMapper, mapName)
        }
    }
    override fun configure() {
        val hazelcastStorage = HazelcastStorage(objectMapper, hazelcastInstance, mapName)
        val hazelcastSessionStorage = HazelcastSessionStorage(hazelcastStorage)
        bind(Storage::class.java).toInstance(hazelcastSessionStorage)
        bind(SessionStorage::class.java).toInstance(hazelcastSessionStorage)
    }
}

class OasStubHazelcastPersistentStorageModule
    private constructor(private val hazelcastInstance: HazelcastInstance,
                        private val objectMapper: ObjectMapper,
                        private val mapName: String): AbstractModule() {
    companion object {
        @JvmStatic
        fun createModuleCreator(hazelcastInstance: HazelcastInstance, mapName: String) = OasStubPersistentStorageModuleCreator { objectMapper ->
            OasStubHazelcastPersistentStorageModule(hazelcastInstance, objectMapper, mapName)
        }
    }
    override fun configure() {
        val hazelcastStorage = HazelcastStorage(objectMapper, hazelcastInstance, mapName)
        bind(PersistentStorage::class.java).toInstance(HazelcastPersistentStorage(hazelcastStorage))
    }
}