package io.github.ktakashi.oas.server.options

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import io.github.ktakashi.oas.storages.inmemory.InMemoryPersistentStorage
import io.github.ktakashi.oas.storages.inmemory.InMemorySessionStorage
import java.security.KeyStore

data class OasStubServerOptions
internal constructor(val port: Int,
                     val httpsPort: Int,
                     val stubPath: String,
                     val adminPath: String,
                     val ssl: SSL?,
                     val enableAdmin: Boolean,
                     val enableAccessLog: Boolean,
                     val objectMapper: ObjectMapper,
                     val persistentStorage: PersistentStorage,
                     val sessionStorage: SessionStorage)
{
    companion object {
        data class Builder
            internal constructor(private var port: Int = 8080,
                                 private var httpsPort: Int = -1,
                                 private var stubPath: String = "/oas",
                                 private var adminPath: String = "/__admin",
                                 private var ssl: SSL? = null,
                                 private var enableAdmin: Boolean = true,
                                 private var enableAccessLog: Boolean = false,
                                 private var objectMapper: ObjectMapper = ObjectMapper(),
                                 private var persistentStorage: PersistentStorage = InMemoryPersistentStorage(),
                                 private var sessionStorage: SessionStorage = InMemorySessionStorage()) {
            fun port(port: Int) = apply { this.port = port }
            fun httpsPort(httpsPort: Int) = apply { this.httpsPort = httpsPort }
            fun stubPath(stubPath: String) = apply { this.stubPath = stubPath }
            fun adminPath(adminPath: String) = apply { this.adminPath = adminPath }
            fun ssl(ssl: SSL) = apply { this.ssl = ssl }
            fun enableAdmin(enableAdmin: Boolean) = apply { this.enableAdmin = enableAdmin }
            fun enableAccessLog(enableAccessLog: Boolean) = apply { this.enableAccessLog = enableAccessLog }
            fun objectMapper(objectMapper: ObjectMapper) = apply { this.objectMapper = objectMapper }
            fun persistentStorage(persistentStorage: PersistentStorage) = apply { this.persistentStorage = persistentStorage }
            fun sessionStorage(sessionStorage: SessionStorage) = apply { this.sessionStorage = sessionStorage }

            fun build() = OasStubServerOptions(port, httpsPort, stubPath, adminPath, ssl, enableAdmin,
                enableAccessLog, objectMapper, persistentStorage, sessionStorage)
        }
        @JvmStatic
        fun builder() = Builder()
    }
}

data class SSL
internal constructor(val keyStore: KeyStore?,
                     val keyAlias: String?,
                     val keyStorePassword: String?,
                     val keyPassword: String?,
                     val trustStore: KeyStore?,
                     val trustStorePassword: String?) {
    companion object {
        data class Builder
            internal constructor(private var keyStore: KeyStore? = null,
                                 private var keyAlias: String? = null,
                                 private var keyStorePassword: String? = null,
                                 private var keyPassword: String? = null,
                                 private var trustStore: KeyStore? = null,
                                 private var trustStorePassword: String? = null) {
            fun keyStore(keyStore: KeyStore) = apply { this.keyStore = keyStore }
            fun keyAlias(keyAlias: String) = apply { this.keyAlias = keyAlias }
            fun keyStorePassword(keyStorePassword: String) = apply { this.keyStorePassword = keyStorePassword }
            fun trustStore(trustStore: KeyStore) = apply { this.trustStore = trustStore }
            fun trustStorePassword(trustStorePassword: String) = apply { this.trustStorePassword = trustStorePassword }

            fun build() = SSL(keyStore, keyAlias, keyStorePassword, keyPassword, trustStore, trustStorePassword)
        }
        @JvmStatic
        fun builder() = Builder()
    }
}
