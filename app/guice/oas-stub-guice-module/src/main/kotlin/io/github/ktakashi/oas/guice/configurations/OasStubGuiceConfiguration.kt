package io.github.ktakashi.oas.guice.configurations

import com.fasterxml.jackson.databind.json.JsonMapper
import io.github.ktakashi.oas.guice.modules.OasStubInMemoryPersistentStorageModule
import io.github.ktakashi.oas.guice.modules.OasStubInMemorySessionStorageModule
import io.github.ktakashi.oas.guice.storages.apis.OasStubPersistentStorageModuleCreator
import io.github.ktakashi.oas.guice.storages.apis.OasStubSessionStorageModuleCreator
import java.util.function.Supplier
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.util.ssl.SslContextFactory

/**
 * OAS Stub configuration.
 *
 * This configuration contains both engine and web config items for simplicity.
 */
data class OasStubConfiguration(
    /**
     * OAS Stub servlet prefix.
     */
    val servletPrefix: String = "/oas",
    /**
     * OAS Stub admin endpoint prefix.
     */
    val adminPrefix: String = "/__admin",
    /**
     * Parallelism of the servlet execution.
     */
    val parallelism: Int = Runtime.getRuntime().availableProcessors()
)

/**
 * Object mapper builder customizer.
 *
 * This functional interface is applied after the base object mapper
 * builder is constructed.
 *
 * The base object mapper builder contains [KotlinModule], [JavaTimeModule]
 * and [Jdk8Module]
 */
fun interface ObjectMapperBuilderCustomizer {
    /**
     * Customization method.
     *
     * @param jacksonBuilder The builder
     */
    fun customize(jacksonBuilder: JsonMapper.Builder)
}

/**
 * OAS Stub configuration.
 *
 * This configuration is used to construct engine components.
 */
open class OasStubGuiceConfiguration
private constructor(
    /**
     * OAS Stub configuration
     */
    val oasStubConfiguration: OasStubConfiguration,
    /**
     * Session storage module creator.
     */
    val sessionStorageModuleCreator: OasStubSessionStorageModuleCreator,
    /**
     * Persistent storage module creator
     */
    val persistentStorageModuleCreator: OasStubPersistentStorageModuleCreator,
    /**
     * Object mapper builder customizer
     */
    val objectMapperBuilderCustomizer: ObjectMapperBuilderCustomizer) {

    constructor(builder: OasStubGuiceConfigurationBuilder<*>) : this(builder.oasStubConfiguration, builder.sessionStorageModuleCreator, builder.persistentStorageModuleCreator, builder.objectMapperBuilderCustomizer)

    companion object {
        /**
         * Creates a builder of [OasStubGuiceConfiguration]
         */
        @JvmStatic
        fun builder() = OasStubGuiceConfigurationBuilder()
    }
    interface Builder<T: OasStubGuiceConfiguration, U: Builder<T, U>>

    @Suppress("UNCHECKED_CAST") // Unfortunately we need to put this :(
    open class OasStubGuiceConfigurationBuilder<T: OasStubGuiceConfigurationBuilder<T>>: Builder<OasStubGuiceConfiguration, OasStubGuiceConfigurationBuilder<T>> {
        var oasStubConfiguration: OasStubConfiguration = OasStubConfiguration()
            private set
        var sessionStorageModuleCreator: OasStubSessionStorageModuleCreator = OasStubInMemorySessionStorageModule.CREATOR
            private set
        var persistentStorageModuleCreator: OasStubPersistentStorageModuleCreator = OasStubInMemoryPersistentStorageModule.CREATOR
            private set

        var objectMapperBuilderCustomizer: ObjectMapperBuilderCustomizer = ObjectMapperBuilderCustomizer {  }

        /**
         * Sets [OasStubConfiguration]
         */
        fun oasStubConfiguration(oasStubConfiguration: OasStubConfiguration): T = apply {
            this.oasStubConfiguration = oasStubConfiguration
        } as T

        /**
         * Sets [OasStubSessionStorageModuleCreator]
         */
        fun sessionStorageModuleCreator(sessionStorageModuleCreator: OasStubSessionStorageModuleCreator): T = apply {
            this.sessionStorageModuleCreator = sessionStorageModuleCreator
        } as T

        /**
         * Sets [OasStubPersistentStorageModuleCreator]
         */
        fun persistentStorageModuleCreator(persistentStorageModuleCreator: OasStubPersistentStorageModuleCreator): T = apply {
            this.persistentStorageModuleCreator = persistentStorageModuleCreator
        } as T

        /**
         * Sets [ObjectMapperBuilderCustomizer]
         */
        fun objectMapperBuilderCustomizer(objectMapperBuilderCustomizer: ObjectMapperBuilderCustomizer): T = apply {
            this.objectMapperBuilderCustomizer = objectMapperBuilderCustomizer
        } as T

        /**
         * Builds the configuration
         */
        open fun build() = OasStubGuiceConfiguration(this)
    }

}

/**
 * OAS Stub web configuration.
 *
 * This configuration is used to construct engine and web components.
 */
open class OasStubGuiceWebConfiguration
private constructor(builder: OasStubGuiceConfigurationBuilder<*>,
                    /**
                     * OAS Stub admin resource customizer
                     */
                    val resourceConfigCustomizers: Set<ResourceConfigCustomizer>)
    : OasStubGuiceConfiguration(builder) {
    constructor(builder: OasStubGuiceWebConfigurationBuilder<*>): this(builder, builder.resourceConfigCustomizers)
    companion object {
        /**
         * Creates a builder of [OasStubGuiceWebConfiguration].
         *
         * The returning builder extends the builder for [OasStubGuiceConfiguration.OasStubGuiceConfigurationBuilder]
         */
        @JvmStatic
        fun builder() = OasStubGuiceWebConfigurationBuilder()
    }

    @Suppress("UNCHECKED_CAST") // Unfortunately we need to put this :(
    open class OasStubGuiceWebConfigurationBuilder<T: OasStubGuiceWebConfigurationBuilder<T>>: OasStubGuiceConfigurationBuilder<T>() {
        var resourceConfigCustomizers: Set<ResourceConfigCustomizer> = setOf()
            private set

        /**
         * Sets a set of [ResourceConfigCustomizer]
         */
        fun resourceConfigCustomizers(resourceConfigCustomizers: Set<ResourceConfigCustomizer>): T = apply {
            this.resourceConfigCustomizers = resourceConfigCustomizers
        } as T

        /**
         * Builds the configuration
         */
        override fun build() = OasStubGuiceWebConfiguration(this)
    }
}

/**
 * Server connector configuration
 */
data class OasStubServerConnectorConfiguration
@JvmOverloads constructor(
    /**
     * Name of the connector
     */
    val name: String,
    /**
     * Connecting host, default "0.0.0.0"
     */
    val host: String = "0.0.0.0",
    /**
     * Port number.
     *
     * `0` means random available port
     */
    val port: Int = 8080,
    /**
     * [HttpConfiguration]
     */
    val httpConfiguration: HttpConfiguration = HttpConfiguration(),
    /**
     * [SslContextFactory.Server] supplier.
     *
     * This is relevant only SSL connector. i.e. [httpConfiguration.securePort] > 0
     */
    val sslContextFactorySupplier: SslContextFactorySupplier = DEFAULT_SSL_CONTEXT_SUPPLIER) {

    constructor(builder: OasStubServerConnectorConfigurationBuilder): this(builder.name, builder.host, builder.port, builder.httpConfiguration, builder.sslContextFactorySupplier)
    fun interface SslContextFactorySupplier: Supplier<SslContextFactory.Server>

    companion object {
        @JvmField
        val DEFAULT_SSL_CONTEXT_SUPPLIER = SslContextFactorySupplier {
            SslContextFactory.Server()
        }
        @JvmStatic
        fun builder(name: String) = OasStubServerConnectorConfigurationBuilder(name)
    }

    class OasStubServerConnectorConfigurationBuilder(internal val name: String) {
        internal var host: String = "0.0.0.0"
        internal var port: Int = 8080
        internal var httpConfiguration: HttpConfiguration = HttpConfiguration()
        internal var sslContextFactorySupplier: SslContextFactorySupplier = DEFAULT_SSL_CONTEXT_SUPPLIER

        fun host(host: String) = apply { this.host = host }
        fun port(port: Int) = apply { this.port = port }
        fun httpConfiguration(httpConfiguration: HttpConfiguration) = apply { this.httpConfiguration = httpConfiguration }
        fun sslContextFactorySupplier(sslContextFactorySupplier: SslContextFactorySupplier) = apply { this.sslContextFactorySupplier = sslContextFactorySupplier}
        fun build() = OasStubServerConnectorConfiguration(this)
    }
}

/**
 * OAS Stub server configuration.
 *
 * This configuration is used to construct engine, web and server components.
 */
class OasStubGuiceServerConfiguration(builder: OasStubGuiceServerConfigurationBuilder,
                                      /**
                                       * Jetty server instance supplier.
                                       */
                                      val jettyServerSupplier: JettyServerSupplier,
                                      /**
                                       * List of server connector configurations.
                                       */
                                      val serverConnectors: List<OasStubServerConnectorConfiguration>,
                                      /**
                                       * Set of WebAppContext customizers
                                       */
                                      val jettyWebAppContextCustomizers: Set<JettyWebAppContextCustomizer>)
    : OasStubGuiceWebConfiguration(builder) {
    constructor(builder: OasStubGuiceServerConfigurationBuilder): this(builder, builder.jettyServerSupplier, builder.serverConnectors, builder.jettyWebAppContextCustomizers)

    companion object {
        /**
         * Creates a builder of [OasStubGuiceServerConfiguration]
         *
         * This builder inherits [OasStubGuiceWebConfiguration.OasStubGuiceWebConfigurationBuilder]
         */
        @JvmStatic
        fun builder(): OasStubGuiceServerConfigurationBuilder = OasStubGuiceServerConfigurationBuilder()
    }

    class OasStubGuiceServerConfigurationBuilder: OasStubGuiceWebConfigurationBuilder<OasStubGuiceServerConfigurationBuilder>() {
        var jettyServerSupplier: JettyServerSupplier = JettyServerSupplier.DEFAULT_JETTY_SERVER_SUPPLIER
            private set
        var serverConnectors: List<OasStubServerConnectorConfiguration> = listOf(OasStubServerConnectorConfiguration("default"))
            private set
        var jettyWebAppContextCustomizers: Set<JettyWebAppContextCustomizer> = setOf()
            private set

        /**
         * Sets [JettyServerSupplier]
         */
        fun jettyServerSupplier(jettyServerSupplier: JettyServerSupplier) = apply {
            this.jettyServerSupplier = jettyServerSupplier
        }

        /**
         * Sets a set of [OasStubServerConnectorConfiguration]
         */
        fun serverConnectors(serverConnectors: List<OasStubServerConnectorConfiguration>) = apply {
            this.serverConnectors = serverConnectors
        }

        /**
         * Sets a set of [JettyWebAppContextCustomizer]
         */
        fun jettyWebAppContextCustomizers(jettyWebAppContextCustomizers: Set<JettyWebAppContextCustomizer>) = apply {
            this.jettyWebAppContextCustomizers = jettyWebAppContextCustomizers
        }

        /**
         * Builds the configuration
         */
        override fun build() = OasStubGuiceServerConfiguration(this)
    }
}