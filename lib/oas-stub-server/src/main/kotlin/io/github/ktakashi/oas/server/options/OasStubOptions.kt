package io.github.ktakashi.oas.server.options

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.github.ktakashi.oas.server.api.OasStubApiForwardingResolver
import io.github.ktakashi.oas.server.handlers.OasStubRoutesBuilder
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import io.github.ktakashi.oas.storages.inmemory.InMemoryPersistentStorage
import io.github.ktakashi.oas.storages.inmemory.InMemorySessionStorage
import java.security.KeyStore

/**
 * OAS Stub options.
 */
data class OasStubOptions
internal constructor(val serverOptions: OasStubServerOptions,
                     val stubOptions: OasStubStubOptions)
{
    companion object {
        /**
         * Returns a new OAS Stub options builder.
         */
        @JvmStatic
        fun builder() = Builder()

        /**
         * Default stub path
         */
        const val DEFAULT_STUB_PATH = "/oas"

        /**
         * Default admin path
         */
        const val DEFAULT_ADMIN_PATH = "/__admin"

        /**
         * Default metrics path segment
         */
        const val DEFAULT_METRICS_PATH = "/metrics"

        /**
         * Default records path segment
         */
        const val DEFAULT_RECORDS_PATH = "/records"
    }
    class Builder internal constructor() {
        private val server: OasStubServerOptions.Builder = OasStubServerOptions.builder(this)
        private val stub: OasStubStubOptions.Builder = OasStubStubOptions.builder(this)

        /**
         * Returns server options builder
         */
        fun serverOptions() = server

        /**
         * Returns stub options builder
         */
        fun stubOptions() = stub

        /**
         * Builds the OAS Stub options
         */
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

        /**
         * Sets the HTTP port of the OAS Stub server. Default value is `8080`.
         *
         * 0 means random port.
         */
        fun port(port: Int) = apply { this.port = port }

        /**
         * Sets the HTTPS port of the OAS Stub server. Default value is `-1` (disabled)
         *
         * 0 means random port.
         */
        fun httpsPort(httpsPort: Int) = apply { this.httpsPort = httpsPort }

        /**
         * Enables reactor netty access logging. Default `false`
         */
        fun enableAccessLog(enableAccessLog: Boolean) = apply { this.enableAccessLog = enableAccessLog }

        /**
         * Returns SSL options builder
         */
        fun ssl() = ssl

        /**
         * Returns the parent options builder
         */
        fun parent() = parent
        internal fun build() = OasStubServerOptions(port, httpsPort, ssl.build(), enableAccessLog)
    }

}

data class OasStubStubOptions
internal constructor(internal val stubPath: String,
                     internal val adminPath: String,
                     internal val forwardingPath: String?,
                     internal val metricsPath: String,
                     internal val recordsPath: String,
                     internal val enableAdmin: Boolean,
                     internal val enableMetrics: Boolean,
                     internal val enableRecord: Boolean,
                     internal val objectMapper: ObjectMapper,
                     internal val routesBuilders: List<OasStubRoutesBuilder>,
                     internal val persistentStorage: PersistentStorage,
                     internal val sessionStorage: SessionStorage,
                     internal val staticConfigurations: List<String>,
                     internal val forwardingResolvers: List<OasStubApiForwardingResolver>)
{
    companion object {
        @JvmStatic
        fun builder(parent: OasStubOptions.Builder) = Builder(parent)
    }

    class Builder(private val parent: OasStubOptions.Builder,
                  private var stubPath: String = OasStubOptions.DEFAULT_STUB_PATH,
                  private var adminPath: String = OasStubOptions.DEFAULT_ADMIN_PATH,
                  private var forwardingPath: String? = null,
                  private var metricsPath: String = OasStubOptions.DEFAULT_METRICS_PATH,
                  private var recordsPath: String = OasStubOptions.DEFAULT_RECORDS_PATH,
                  private var enableAdmin: Boolean = true,
                  private var enableMetrics: Boolean = true,
                  private var enableRecords: Boolean = false,
                  private var routesBuilders: MutableList<OasStubRoutesBuilder> = mutableListOf(),
                  private var objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules(),
                  private var persistentStorage: PersistentStorage = InMemoryPersistentStorage(),
                  private var sessionStorage: SessionStorage = InMemorySessionStorage(),
                  private var staticConfigurations: MutableList<String> = mutableListOf(),
                  private var forwardingResolvers: MutableList<OasStubApiForwardingResolver> = mutableListOf()) {
        /**
         * Sets the stub path. Default value is [OasStubOptions.DEFAULT_STUB_PATH]
         */
        fun stubPath(stubPath: String) = apply { this.stubPath = stubPath }

        /**
         * Sets the admin path. Default value is [OasStubOptions.DEFAULT_ADMIN_PATH]
         */
        fun adminPath(adminPath: String) = apply { this.adminPath = adminPath }

        /**
         * Sets the forwarding path. Default disabled.
         */
        fun forwardingPath(forwardingPath: String) = apply { this.forwardingPath = forwardingPath }

        /**
         * Sets the metrics path segment. Default value is [OasStubOptions.DEFAULT_METRICS_PATH]
         */
        fun metricsPath(metricsPath: String) = apply { this.metricsPath = metricsPath }

        /**
         * Sets the records path segment. Default value is [OasStubOptions.DEFAULT_RECORDS_PATH]
         */
        fun recordsPath(recordsPath: String) = apply { this.recordsPath = recordsPath }

        /**
         * Enabling admin endpoints. Default value is `true`
         */
        fun enableAdmin(enableAdmin: Boolean) = apply { this.enableAdmin = enableAdmin }

        /**
         * Enabling metrics endpoints. Default value is `true`
         */
        fun enableMetrics(enableMetrics: Boolean) = apply { this.enableMetrics = enableMetrics }

        /**
         * Enabling records endpoints. Default value is `false`
         */
        fun enableRecords(enableRecords: Boolean) = apply { this.enableRecords = enableRecords }

        /**
         * Sets object mapper to be used.
         *
         * The object mapper will be copied when the option is built. Which means,
         * changing its configuration at runtime is not possible.
         */
        fun objectMapper(objectMapper: ObjectMapper) = apply { this.objectMapper = objectMapper }

        /**
         * Sets persistent storage. Default storage is [InMemoryPersistentStorage]
         */
        fun persistentStorage(persistentStorage: PersistentStorage) = apply { this.persistentStorage = persistentStorage }

        /**
         * Sets session storage. Default storage is [InMemorySessionStorage]
         */
        fun sessionStorage(sessionStorage: SessionStorage) = apply { this.sessionStorage = sessionStorage }

        /**
         * Locations of static configuration
         */
        fun staticConfigurations(staticConfigurations: List<String>) = apply { this.staticConfigurations = staticConfigurations.toMutableList() }

        /**
         * Adds a static configuration location
         */
        fun addStaticConfiguration(configuration: String) = apply { this.staticConfigurations.add(configuration) }
        /**
         * Sets custom routes builders.
         *
         * This replaces the entire builders
         */
        fun routesBuilders(routesBuilders: Collection<OasStubRoutesBuilder>) = apply { this.routesBuilders = routesBuilders.toMutableList() }

        /**
         * Adds a custom routes builder.
         */
        fun addRoutesBuilder(routesBuilder: OasStubRoutesBuilder) = apply { this.routesBuilders.add(routesBuilder) }

        /**
         * Sets forwarding resolvers
         */
        fun forwardingResolvers(forwardingResolvers: List<OasStubApiForwardingResolver>) = apply { this.forwardingResolvers = forwardingResolvers.toMutableList() }

        /**
         * Adds a forwarding resolver
         */
        fun addForwardingResolver(forwardingResolver: OasStubApiForwardingResolver) = apply { this.forwardingResolvers.add(forwardingResolver) }

        /**
         * Returns the parent options builder.
         */
        fun parent() = parent
        internal fun build() = OasStubStubOptions(stubPath, adminPath, forwardingPath,
            metricsPath, recordsPath,
            enableAdmin, enableMetrics, enableRecords,
            objectMapper.copy().registerModules(KotlinModule.Builder().build(), Jdk8Module()),
            routesBuilders,
            persistentStorage, sessionStorage, staticConfigurations, forwardingResolvers)
    }

}

/**
 * The SSL options are used when the [OasStubServerOptions.httpsPort] is `>= 0`.
 * If the options is not set, then the server generates a self-signed certificate
 */
data class OasStubServerSSLOptions
internal constructor(internal val keyStore: KeyStore?,
                     internal val keyAlias: String?,
                     internal val clientAuth: ClientAuth,
                     internal val keyPassword: String?,
                     internal val trustStore: KeyStore?) {
    enum class ClientAuth {
        NONE,
        OPTIONAL,
        REQUIRE,
    }
    companion object {
        @JvmStatic
        fun builder(parent: OasStubServerOptions.Builder) = Builder(parent)
    }

    class Builder
    internal constructor(private val parent: OasStubServerOptions.Builder,
                         private var keyStore: KeyStore? = null,
                         private var keyAlias: String? = null,
                         private var clientAuth: ClientAuth = ClientAuth.NONE,
                         private var keyStorePassword: String? = null,
                         private var trustStore: KeyStore? = null) {
        /**
         * Sets the keystore for TLS key.
         */
        fun keyStore(keyStore: KeyStore) = apply { this.keyStore = keyStore }

        /**
         * Sets the key alias to be used.
         */
        fun keyAlias(keyAlias: String) = apply { this.keyAlias = keyAlias }

        /**
         * Specify client authentication
         */
        fun clientAuth(clientAuth: ClientAuth) = apply { this.clientAuth = clientAuth }

        /**
         * Sets the trust store
         */
        fun trustStore(trustStore: KeyStore) = apply { this.trustStore = trustStore }

        /**
         * Returns the parent options builder
         */
        fun parent() = parent

        internal fun build() = OasStubServerSSLOptions(keyStore, keyAlias, clientAuth, keyStorePassword, trustStore)
    }

}
