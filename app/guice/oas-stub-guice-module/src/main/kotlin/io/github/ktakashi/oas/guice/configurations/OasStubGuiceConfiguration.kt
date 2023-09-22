package io.github.ktakashi.oas.guice.configurations

import com.fasterxml.jackson.databind.json.JsonMapper
import io.github.ktakashi.oas.guice.modules.OasStubInMemoryPersistentStorageModule
import io.github.ktakashi.oas.guice.modules.OasStubInMemorySessionStorageModule
import io.github.ktakashi.oas.guice.storages.apis.OasStubPersistentStorageModuleCreator
import io.github.ktakashi.oas.guice.storages.apis.OasStubSessionStorageModuleCreator
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
                    val sessionStorageModuleCreator: OasStubSessionStorageModuleCreator,
                    val persistentStorageModuleCreator: OasStubPersistentStorageModuleCreator,
                    val objectMapperBuilderCustomizer: ObjectMapperBuilderCustomizer) {

    constructor(builder: EngineConfigurationBuilder<*>) : this(builder.oasStubConfiguration, builder.sessionStorageModuleCreator, builder.persistentStorageModuleCreator, builder.objectMapperBuilderCustomizer)

    companion object {
        @JvmStatic
        fun builder() = EngineConfigurationBuilder()
    }
    interface Builder<T: OasStubGuiceConfiguration, U: Builder<T, U>>

    @Suppress("UNCHECKED_CAST") // Unfortunately we need to put this :(
    open class EngineConfigurationBuilder<T: EngineConfigurationBuilder<T>>: Builder<OasStubGuiceConfiguration, EngineConfigurationBuilder<T>> {
        var oasStubConfiguration: OasStubConfiguration = OasStubConfiguration()
            private set
        var sessionStorageModuleCreator: OasStubSessionStorageModuleCreator = OasStubInMemorySessionStorageModule.CREATOR
            private set
        var persistentStorageModuleCreator: OasStubPersistentStorageModuleCreator = OasStubInMemoryPersistentStorageModule.CREATOR
            private set

        var objectMapperBuilderCustomizer: ObjectMapperBuilderCustomizer = ObjectMapperBuilderCustomizer {  }

        fun oasStubConfiguration(oasStubConfiguration: OasStubConfiguration): T = apply {
            this.oasStubConfiguration = oasStubConfiguration
        } as T

        fun sessionStorageModuleCreator(sessionStorageModuleCreator: OasStubSessionStorageModuleCreator): T = apply {
            this.sessionStorageModuleCreator = sessionStorageModuleCreator
        } as T

        fun persistentStorageModuleCreator(persistentStorageModuleCreator: OasStubPersistentStorageModuleCreator): T = apply {
            this.persistentStorageModuleCreator = persistentStorageModuleCreator
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
                                      val jettyWebAppContextCustomizers: Set<JettyWebAppContextCustomizer>)
    : OasStubGuiceWebConfiguration(builder) {
    constructor(builder: ServerConfigurationBuilder): this(builder, builder.jettyServerSupplier, builder.serverConnectors, builder.jettyWebAppContextCustomizers)

    companion object {
        @JvmStatic
        fun builder(): ServerConfigurationBuilder = ServerConfigurationBuilder()
    }

    class ServerConfigurationBuilder: WebConfigurationBuilder<ServerConfigurationBuilder>() {
        var jettyServerSupplier: JettyServerSupplier = defaultJettyServerSupplier
            private set
        var serverConnectors: List<OasStubServerConnectorConfiguration> = listOf(OasStubServerConnectorConfiguration("default"))
            private set
        var jettyWebAppContextCustomizers: Set<JettyWebAppContextCustomizer> = setOf()
            private set

        fun jettyServerSupplier(jettyServerSupplier: JettyServerSupplier) = apply {
            this.jettyServerSupplier = jettyServerSupplier
        }

        fun serverConnectors(serverConnectors: List<OasStubServerConnectorConfiguration>) = apply {
            this.serverConnectors = serverConnectors
        }

        fun jettyWebAppContextCustomizers(jettyWebAppContextCustomizers: Set<JettyWebAppContextCustomizer>) = apply {
            this.jettyWebAppContextCustomizers = jettyWebAppContextCustomizers
        }

        override fun build() = OasStubGuiceServerConfiguration(this)
    }
}