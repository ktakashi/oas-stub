package io.github.ktakashi.oas.server.options

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.server.handlers.OasStubRoutesBuilder
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import io.github.ktakashi.oas.storages.inmemory.InMemoryPersistentStorage
import io.github.ktakashi.oas.storages.inmemory.InMemorySessionStorage
import java.security.KeyStore

data class OasStubOptions
internal constructor(val serverOptions: OasStubServerOptions,
                     val stubOptions: OasStubStubOptions)
{
    companion object {
        @JvmStatic
        fun builder() = Builder()
    }
    class Builder internal constructor() {
        private val server: OasStubServerOptions.Builder = OasStubServerOptions.builder(this)
        private val stub: OasStubStubOptions.Builder = OasStubStubOptions.builder(this)

        fun serverOptions() = server
        fun stubOptions() = stub

        fun build(): OasStubOptions = OasStubOptions(server.build(), stub.build())
    }
}

data class OasStubServerOptions
internal constructor(val port: Int,
                     val httpsPort: Int,
                     val ssl: OasStubServerSSLOptions?,
                     val enableAccessLog: Boolean)
{
    companion object {
        @JvmStatic
        internal fun builder(parent: OasStubOptions.Builder) = Builder(parent)
    }
    class Builder
    internal constructor(private val parent: OasStubOptions.Builder,
                         private var port: Int = 8080,
                         private var httpsPort: Int = -1,
                         private var enableAccessLog: Boolean = false) {
        private val ssl: OasStubServerSSLOptions.Builder = OasStubServerSSLOptions.builder(this)

        fun port(port: Int) = apply { this.port = port }
        fun httpsPort(httpsPort: Int) = apply { this.httpsPort = httpsPort }
        fun enableAccessLog(enableAccessLog: Boolean) = apply { this.enableAccessLog = enableAccessLog }
        fun ssl() = ssl
        fun options() = parent
        internal fun build() = OasStubServerOptions(port, httpsPort, ssl.build(), enableAccessLog)
    }

}

data class OasStubStubOptions
internal constructor(internal val stubPath: String,
                     internal val adminPath: String,
                     internal val metricsPath: String,
                     internal val enableAdmin: Boolean,
                     internal val enableMetrics: Boolean,
                     internal val objectMapper: ObjectMapper,
                     internal val routesBuilders: List<OasStubRoutesBuilder>,
                     internal val persistentStorage: PersistentStorage,
                     internal val sessionStorage: SessionStorage)
{
    companion object {
        @JvmStatic
        fun builder(parent: OasStubOptions.Builder) = Builder(parent)
    }

    class Builder(private val parent: OasStubOptions.Builder,
                  private var stubPath: String = "/oas",
                  private var adminPath: String = "/__admin",
                  private var metricsPath: String = "/metrics",
                  private var enableAdmin: Boolean = true,
                  private var enableMetrics: Boolean = true,
                  private var routesBuilders: MutableList<OasStubRoutesBuilder> = mutableListOf(),
                  private var objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules(),
                  private var persistentStorage: PersistentStorage = InMemoryPersistentStorage(),
                  private var sessionStorage: SessionStorage = InMemorySessionStorage()) {
        fun stubPath(stubPath: String) = apply { this.stubPath = stubPath }
        fun adminPath(adminPath: String) = apply { this.adminPath = adminPath }
        fun metricsPath(metricsPath: String) = apply { this.metricsPath = metricsPath }
        fun enableAdmin(enableAdmin: Boolean) = apply { this.enableAdmin = enableAdmin }
        fun enableMetrics(enableMetrics: Boolean) = apply { this.enableMetrics = enableMetrics }
        fun objectMapper(objectMapper: ObjectMapper) = apply { this.objectMapper = objectMapper }
        fun persistentStorage(persistentStorage: PersistentStorage) = apply { this.persistentStorage = persistentStorage }
        fun sessionStorage(sessionStorage: SessionStorage) = apply { this.sessionStorage = sessionStorage }
        fun routesBuilders(routesBuilders: List<OasStubRoutesBuilder>) = apply { this.routesBuilders = routesBuilders.toMutableList() }
        fun addRoutesBuilder(routesBuilder: OasStubRoutesBuilder) = apply { this.routesBuilders.add(routesBuilder) }
        fun options() = parent
        internal fun build() = OasStubStubOptions(stubPath, adminPath, metricsPath, enableAdmin, enableMetrics,
            objectMapper, routesBuilders, persistentStorage, sessionStorage)
    }

}

data class OasStubServerSSLOptions
internal constructor(internal val keyStore: KeyStore?,
                     internal val keyAlias: String?,
                     internal val keyStorePassword: String?,
                     internal val keyPassword: String?,
                     internal val trustStore: KeyStore?,
                     internal val trustStorePassword: String?) {
    companion object {
        @JvmStatic
        fun builder(parent: OasStubServerOptions.Builder) = Builder(parent)
    }

    class Builder
    internal constructor(private val parent: OasStubServerOptions.Builder,
                         private var keyStore: KeyStore? = null,
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

        fun serverOptions() = parent

        internal fun build() = OasStubServerSSLOptions(keyStore, keyAlias, keyStorePassword, keyPassword, trustStore, trustStorePassword)
    }

}
