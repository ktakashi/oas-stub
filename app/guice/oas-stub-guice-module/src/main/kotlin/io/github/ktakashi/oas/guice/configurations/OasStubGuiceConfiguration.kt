package io.github.ktakashi.oas.guice.configurations

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

interface OasStubGuiceConfiguration {
    val oasStubConfiguration: OasStubConfiguration
    val sessionStorageModule: OasStubSessionStorageModule
    val persistentStorageModule: OasStubPersistentStorageModule
    val resourceConfigCustomizers: Set<ResourceConfigCustomizer>

    interface Builder<T: OasStubGuiceConfiguration, U: Builder<T, U>> {
        fun oasStubConfiguration(oasStubConfiguration: OasStubConfiguration): U

        fun sessionStorageModule(sessionStorageModule: OasStubSessionStorageModule): U

        fun persistentStorageModule(persistentStorageModule: OasStubPersistentStorageModule): U

        fun resourceConfigCustomizers(resourceConfigCustomizers: Set<ResourceConfigCustomizer>): U

    }
}

open class OasStubGuiceWebConfiguration(override val oasStubConfiguration: OasStubConfiguration,
                                        override val sessionStorageModule: OasStubSessionStorageModule,
                                        override val persistentStorageModule: OasStubPersistentStorageModule,
                                        override val resourceConfigCustomizers: Set<ResourceConfigCustomizer>)
    : OasStubGuiceConfiguration {
    companion object {
        @JvmStatic
        fun builder() = WebConfigurationBuilder()
    }

    @Suppress("UNCHECKED_CAST") // Unfortunately we need to put this :(
    open class WebConfigurationBuilder<T: WebConfigurationBuilder<T>>: OasStubGuiceConfiguration.Builder<OasStubGuiceWebConfiguration, T> {
        var oasStubConfiguration: OasStubConfiguration = OasStubConfiguration()
            private set
        var sessionStorageModule: OasStubSessionStorageModule = OasStubInMemorySessionStorageModule()
            private set
        var persistentStorageModule: OasStubPersistentStorageModule = OasStubInMemoryPersistentStorageModule()
            private set

        var resourceConfigCustomizers: Set<ResourceConfigCustomizer> = setOf()
            private set

        override fun oasStubConfiguration(oasStubConfiguration: OasStubConfiguration): T = apply {
            this.oasStubConfiguration = oasStubConfiguration
        } as T

        override fun sessionStorageModule(sessionStorageModule: OasStubSessionStorageModule): T = apply {
            this.sessionStorageModule = sessionStorageModule
        } as T

        override fun persistentStorageModule(persistentStorageModule: OasStubPersistentStorageModule): T = apply {
            this.persistentStorageModule = persistentStorageModule
        } as T

        override fun resourceConfigCustomizers(resourceConfigCustomizers: Set<ResourceConfigCustomizer>): T = apply {
            this.resourceConfigCustomizers = resourceConfigCustomizers
        } as T

        open fun build() = OasStubGuiceWebConfiguration(
            oasStubConfiguration,
            sessionStorageModule,
            persistentStorageModule,
            resourceConfigCustomizers
        )
    }
}

data class OasStubServerConnectorConfiguration(val name: String, val host: String = "0.0.0.0", val port: Int = 0, val httpConfiguration: HttpConfiguration = HttpConfiguration())

class OasStubGuiceServerConfiguration(val jettyServerSupplier: JettyServerSupplier,
                                      val serverConnectors: List<OasStubServerConnectorConfiguration>,
                                      val jettyWebAppContextCustomizer: JettyWebAppContextCustomizer,
                                      oasStubConfiguration: OasStubConfiguration,
                                      sessionStorageModule: OasStubSessionStorageModule,
                                      persistentStorageModule: OasStubPersistentStorageModule,
                                      resourceConfigCustomizers: Set<ResourceConfigCustomizer>)
    : OasStubGuiceWebConfiguration(oasStubConfiguration, sessionStorageModule, persistentStorageModule, resourceConfigCustomizers) {
    companion object {
        @JvmStatic
        fun builder(): ServerConfigurationBuilder = ServerConfigurationBuilder()
    }

    class ServerConfigurationBuilder: WebConfigurationBuilder<ServerConfigurationBuilder>() {
        private var jettyServerSupplier: JettyServerSupplier = defaultJettyServerSupplier
        private var serverConnectors: List<OasStubServerConnectorConfiguration> = listOf(OasStubServerConnectorConfiguration("default"))
        private var jettyWebAppContextCustomizer: JettyWebAppContextCustomizer = JettyWebAppContextCustomizer { }

        fun jettyServerSupplier(jettyServerSupplier: JettyServerSupplier) = apply {
            this.jettyServerSupplier = jettyServerSupplier
        }

        fun serverConnectors(serverConnectors: List<OasStubServerConnectorConfiguration>) = apply {
            this.serverConnectors = serverConnectors
        }

        fun jettyWebAppContextCustomizer(jettyWebAppContextCustomizer: JettyWebAppContextCustomizer) = apply {
            this.jettyWebAppContextCustomizer = jettyWebAppContextCustomizer
        }

        override fun build() = OasStubGuiceServerConfiguration(
            jettyServerSupplier,
            serverConnectors,
            jettyWebAppContextCustomizer,
            oasStubConfiguration,
            sessionStorageModule,
            persistentStorageModule,
            resourceConfigCustomizers
        )
    }
}