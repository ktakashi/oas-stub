package io.github.ktakashi.oas.guice.cucumber.hazelcast

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.inject.Injector
import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import com.hazelcast.config.SerializerConfig
import com.hazelcast.core.HazelcastInstance
import io.cucumber.guice.CucumberModules
import io.cucumber.guice.InjectorSource
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceServerConfiguration
import io.github.ktakashi.oas.guice.configurations.OasStubServerConnectorConfiguration
import io.github.ktakashi.oas.guice.cucumber.CucumberTestModule
import io.github.ktakashi.oas.guice.cucumber.webAppContextCustomizer
import io.github.ktakashi.oas.guice.injector.createServerInjector
import io.github.ktakashi.oas.guice.storages.hazelcast.modules.OasStubHazelcastPersistentStorageModule
import io.github.ktakashi.oas.guice.storages.hazelcast.modules.OasStubHazelcastSessionStorageModule
import io.github.ktakashi.oas.storages.hazelcast.JsonSerializer

class HazelcastInjectorSource: InjectorSource {
    override fun getInjector(): Injector = createHazelcastInstance().let { hazelcastInstance ->
        createServerInjector(
            OasStubGuiceServerConfiguration.builder()
                .sessionStorageModuleCreator(OasStubHazelcastSessionStorageModule.createModuleCreator(hazelcastInstance, "session"))
                .persistentStorageModuleCreator(OasStubHazelcastPersistentStorageModule.createModuleCreator(hazelcastInstance, "persistent"))
                .jettyWebAppContextCustomizers(setOf(webAppContextCustomizer))
                .serverConnectors(listOf(OasStubServerConnectorConfiguration.builder("http").port(0).build()))
                .build(),
            CucumberTestModule(),
            CucumberModules.createScenarioModule())
    }
}

private fun createHazelcastInstance(): HazelcastInstance =
    HazelcastClient.newHazelcastClient(ClientConfig().apply {
        // this property is populated by the Hazelcast plugin
        val nodeIps = (System.getProperty("hazelcast.cluster.members") as String).split(",")
        networkConfig.addresses = nodeIps.toList()
        networkConfig.isSmartRouting = true
        // must have this JsonSerializer...
        serializationConfig.addSerializerConfig(SerializerConfig().apply {
            implementation = JsonSerializer(ObjectMapper()
                .registerModule(KotlinModule.Builder().build())
                .registerModule(JavaTimeModule())
                .registerModule(Jdk8Module()),
                Int.MAX_VALUE)
            typeClass = JsonNode::class.java
        })
    })
