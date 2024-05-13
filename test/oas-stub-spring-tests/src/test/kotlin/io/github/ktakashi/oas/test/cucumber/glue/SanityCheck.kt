package io.github.ktakashi.oas.test.cucumber.glue

import io.cucumber.java.en.Then
import io.cucumber.spring.CucumberContextConfiguration
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import io.github.ktakashi.oas.storages.apis.conditions.OAS_STUB_STORAGE_TYPE_PERSISTENT
import io.github.ktakashi.oas.storages.apis.conditions.OAS_STUB_STORAGE_TYPE_SESSION
import io.github.ktakashi.oas.storages.hazelcast.HazelcastPersistentStorage
import io.github.ktakashi.oas.storages.hazelcast.HazelcastSessionStorage
import io.github.ktakashi.oas.storages.inmemory.InMemoryPersistentStorage
import io.github.ktakashi.oas.storages.inmemory.InMemorySessionStorage
import io.github.ktakashi.oas.storages.mongodb.MongodbPersistentStorage
import io.github.ktakashi.oas.storages.mongodb.MongodbSessionStorage
import io.github.ktakashi.oas.test.AutoConfigureOasStubServer
import io.github.ktakashi.oas.test.OasStubTestProperties
import io.github.ktakashi.oas.test.cucumber.TestContext
import io.github.ktakashi.oas.test.cucumber.TestContextSupplier
import org.junit.jupiter.api.Assertions.assertTrue
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@CucumberContextConfiguration
@EnableAutoConfiguration
@AutoConfigureOasStubServer(port = 0)
class SanityCheck(private val sessionStorage: SessionStorage,
                  private val persistentStorage: PersistentStorage,
                  @Value("\${${OAS_STUB_STORAGE_TYPE_PERSISTENT}:inmemory}") private val persistentStorageType: String,
                  @Value("\${${OAS_STUB_STORAGE_TYPE_SESSION}:inmemory}") private val sessionStorageType: String) {

    @TestConfiguration
    class TestConfig {
        @Bean @Lazy
        fun testContextSupplier(@Value("\${${OasStubTestProperties.OAS_STUB_SERVER_PROPERTY_PREFIX}.port}") localPort: Int, properties: OasStubTestProperties) = TestContextSupplier {
            TestContext("http://localhost:$localPort", properties.server.stubPrefix, properties.server.adminPrefix)
        }
    }

    @Then("I have proper storages")
    fun `I have proper storages`() {
        println("$persistentStorageType = $persistentStorage")
        when (persistentStorageType) {
            "hazelcast" -> assertTrue(persistentStorage is HazelcastPersistentStorage)
            "mongodb" -> assertTrue(persistentStorage is MongodbPersistentStorage)
            else -> assertTrue(persistentStorage is InMemoryPersistentStorage)
        }

        println("$sessionStorageType = $sessionStorage")
        when (sessionStorageType) {
            "hazelcast" -> assertTrue(sessionStorage is HazelcastSessionStorage)
            "mongodb" -> assertTrue(sessionStorage is MongodbSessionStorage)
            else -> assertTrue(sessionStorage is InMemorySessionStorage)
        }
    }
}