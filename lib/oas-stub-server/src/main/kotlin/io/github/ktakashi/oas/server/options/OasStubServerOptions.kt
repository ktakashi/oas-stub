package io.github.ktakashi.oas.server.options

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import io.github.ktakashi.oas.storages.inmemory.InMemoryPersistentStorage
import io.github.ktakashi.oas.storages.inmemory.InMemorySessionStorage
import java.security.KeyStore

data class OasStubServerOptions
@JvmOverloads constructor(val port: Int = 8080,
                          val httpsPort: Int = -1,
                          val stubPath: String = "/oas",
                          val adminPath: String = "/admin",
                          val ssl: SSL? = null,
                          val objectMapper: ObjectMapper = ObjectMapper(),
                          val persistentStorage: PersistentStorage = InMemoryPersistentStorage(),
                          val sessionStorage: SessionStorage = InMemorySessionStorage())

data class SSL
@JvmOverloads constructor(val keyStore: KeyStore,
                          val keyAlias: String,
                          val keyStorePassword: String,
                          val keyPassword: String,
                          val trustStore: KeyStore? = null,
                          val trustStorePassword: String? = null)