package io.github.ktakashi.oas.guice.storages.hazelcast.modules

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.AbstractModule
import com.hazelcast.core.HazelcastInstance
import io.github.ktakashi.oas.guice.storages.apis.OasStubPersistentStorageModule
import io.github.ktakashi.oas.guice.storages.apis.OasStubSessionStorageModule
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import io.github.ktakashi.oas.storages.hazelcast.HazelcastPersistentStorage
import io.github.ktakashi.oas.storages.hazelcast.HazelcastSessionStorage
import io.github.ktakashi.oas.storages.hazelcast.HazelcastStorage

class OasStubHazelcastSessionStorageModule(private val hazelcastInstance: HazelcastInstance,
                                           private val objectMapper: ObjectMapper,
                                           private val mapName: String): AbstractModule(), OasStubSessionStorageModule {
    override fun configure() {
        val hazelcastStorage = HazelcastStorage(objectMapper, hazelcastInstance, mapName)
        bind(SessionStorage::class.java).toInstance(HazelcastSessionStorage(hazelcastStorage))
    }
}

class OasStubHazelcastPersistentStorageModule(private val hazelcastInstance: HazelcastInstance,
                                              private val objectMapper: ObjectMapper,
                                              private val mapName: String): AbstractModule(), OasStubPersistentStorageModule {
    override fun configure() {
        val hazelcastStorage = HazelcastStorage(objectMapper, hazelcastInstance, mapName)
        bind(PersistentStorage::class.java).toInstance(HazelcastPersistentStorage(hazelcastStorage))
    }
}