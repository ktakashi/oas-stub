package io.github.ktakashi.oas.guice.configurations

import com.fasterxml.jackson.databind.json.JsonMapper
import io.github.ktakashi.oas.guice.modules.OasStubInMemoryPersistentStorageModule
import io.github.ktakashi.oas.guice.modules.OasStubInMemorySessionStorageModule
import io.github.ktakashi.oas.guice.storages.apis.OasStubPersistentStorageModule
import io.github.ktakashi.oas.guice.storages.apis.OasStubSessionStorageModule
import org.eclipse.jetty.server.HttpConfiguration

data class OasStubConfiguration(
    val servletPrefix: String = "/oas",
    val adminPrefix: String = "/__admin",
    val parallelism: Int = Runtime.getRuntime().availableProcessors()
)

fun interface ObjectMapperBuilderCustomizer {
    fun customize(jacksonBuilder: JsonMapper.Builder)
}

open class OasStubGuiceConfiguration
private constructor(val oasStubConfiguration: OasStubConfiguration,
                    val sessionStorageModule: OasStubSessionStorageModule,
                    val persistentStorageModule: OasStubPersistentStorageModule,
                    val objectMapperBuilderCustomizer: ObjectMapperBuilderCustomizer) {

    constructor(builder: EngineConfigurationBuilder<*>) : this(builder.oasStubConfiguration, builder.sessionStorageModule, builder.persistentStorageModule, builder.objectMapperBuilderCustomizer)

    companion object {
        @JvmStatic
        fun builder() = EngineConfigurationBuilder()
    }
    interface Builder<T: OasStubGuiceConfiguration, U: Builder<T, U>>

    @Suppress("UNCHECKED_CAST") // Unfortunately we need to put this :(
    open class EngineConfigurationBuilder<T: EngineConfigurationBuilder<T>>: Builder<OasStubGuiceConfiguration, EngineConfigurationBuilder<T>> {
        var oasStubConfiguration: OasStubConfiguration = OasStubConfiguration()
            private set
        var sessionStorageModule: OasStubSessionStorageModule = OasStubInMemorySessionStorageModule()
            private set
        var persistentStorageModule: OasStubPersistentStorageModule = OasStubInMemoryPersistentStorageModule()
            private set

        var objectMapperBuilderCustomizer: ObjectMapperBuilderCustomizer = ObjectMapperBuilderCustomizer {  }

        fun oasStubConfiguration(oasStubConfiguration: OasStubConfiguration): T = apply {
            this.oasStubConfiguration = oasStubConfiguration
        } as T

        fun sessionStorageModule(sessionStorageModule: OasStubSessionStorageModule): T = apply {
            this.sessionStorageModule = sessionStorageModule
        } as T

        fun persistentStorageModule(persistentStorageModule: OasStubPersistentStorageModule): T = apply {
            this.persistentStorageModule = persistentStorageModule
        } as T

        fun objectMapperBuilderCustomizer(objectMapperBuilderCustomizer: ObjectMapperBuilderCustomizer): T = apply {
            this.objectMapperBuilderCustomizer = objectMapperBuilderCustomizer
        } as T

        open fun build() = OasStubGuiceConfiguration(this)
    }

}

open class OasStubGuiceWebConfiguration
private constructor(builder: EngineConfigurationBuilder<*>,
                    val resourceConfigCustomizers: Set<ResourceConfigCustomizer>)
    : OasStubGuiceConfiguration(builder) {
    constructor(builder: WebConfigurationBuilder<*>): this(builder, builder.resourceConfigCustomizers)
    companion object {
        @JvmStatic
        fun builder() = WebConfigurationBuilder()
    }

    @Suppress("UNCHECKED_CAST") // Unfortunately we need to put this :(
    open class WebConfigurationBuilder<T: WebConfigurationBuilder<T>>: EngineConfigurationBuilder<T>() {
        var resourceConfigCustomizers: Set<ResourceConfigCustomizer> = setOf()
            private set

        fun resourceConfigCustomizers(resourceConfigCustomizers: Set<ResourceConfigCustomizer>): T = apply {
            this.resourceConfigCustomizers = resourceConfigCustomizers
        } as T

        override fun build() = OasStubGuiceWebConfiguration(this)
    }
}

data class OasStubServerConnectorConfiguration(val name: String, val host: String = "0.0.0.0", val port: Int = 0, val httpConfiguration: HttpConfiguration = HttpConfiguration())

class OasStubGuiceServerConfiguration(builder: ServerConfigurationBuilder,
                                      val jettyServerSupplier: JettyServerSupplier,
                                      val serverConnectors: List<OasStubServerConnectorConfiguration>,
                                      val jettyWebAppContextCustomizer: JettyWebAppContextCustomizer)
    : OasStubGuiceWebConfiguration(builder) {
    constructor(builder: ServerConfigurationBuilder): this(builder, builder.jettyServerSupplier, builder.serverConnectors, builder.jettyWebAppContextCustomizer)

    companion object {
        @JvmStatic
        fun builder(): ServerConfigurationBuilder = ServerConfigurationBuilder()
    }

    class ServerConfigurationBuilder: WebConfigurationBuilder<ServerConfigurationBuilder>() {
        var jettyServerSupplier: JettyServerSupplier = defaultJettyServerSupplier
            private set
        var serverConnectors: List<OasStubServerConnectorConfiguration> = listOf(OasStubServerConnectorConfiguration("default"))
            private set
        var jettyWebAppContextCustomizer: JettyWebAppContextCustomizer = JettyWebAppContextCustomizer { }
            private set

        fun jettyServerSupplier(jettyServerSupplier: JettyServerSupplier) = apply {
            this.jettyServerSupplier = jettyServerSupplier
        }

        fun serverConnectors(serverConnectors: List<OasStubServerConnectorConfiguration>) = apply {
            this.serverConnectors = serverConnectors
        }

        fun jettyWebAppContextCustomizer(jettyWebAppContextCustomizer: JettyWebAppContextCustomizer) = apply {
            this.jettyWebAppContextCustomizer = jettyWebAppContextCustomizer
        }

        override fun build() = OasStubGuiceServerConfiguration(this)
    }
}