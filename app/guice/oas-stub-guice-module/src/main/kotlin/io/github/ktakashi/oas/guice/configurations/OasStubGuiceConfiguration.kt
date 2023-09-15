package io.github.ktakashi.oas.guice.configurations

import io.github.ktakashi.oas.guice.modules.OasStubInMemoryPersistentStorageModule
import io.github.ktakashi.oas.guice.modules.OasStubInMemorySessionStorageModule
import io.github.ktakashi.oas.guice.modules.OasStubPersistentStorageModule
import io.github.ktakashi.oas.guice.modules.OasStubSessionStorageModule

data class OasStubGuiceConfiguration(val servletPrefix: String,
                                     val adminServletPrefix: String,
                                     val jettyServerSupplier: JettyServerSupplier,
                                     val sessionStorageModule: OasStubSessionStorageModule,
                                     val persistentStorageModule: OasStubPersistentStorageModule) {
    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    class Builder {
        private var servletPrefix: String = "/oas"
        private var adminServletPrefix: String = "/__admin"
        private var jettyServerSupplier = defaultJettyServerSupplier
        private var sessionStorageModule: OasStubSessionStorageModule = OasStubInMemorySessionStorageModule()
        private var persistentStorageModule: OasStubPersistentStorageModule = OasStubInMemoryPersistentStorageModule()

        fun servletPrefix(prefix: String) = apply {
            this.servletPrefix = prefix
        }

        fun adminServletPrefix(prefix: String) = apply {
            this.adminServletPrefix = prefix
        }

        fun jettyServerSupplier(jettyServerSupplier: JettyServerSupplier) = apply {
            this.jettyServerSupplier = jettyServerSupplier
        }

        fun sessionStorageModule(sessionStorageModule: OasStubSessionStorageModule) = apply {
            this.sessionStorageModule = sessionStorageModule
        }

        fun persistentStorageModule(persistentStorageModule: OasStubPersistentStorageModule) = apply {
            this.persistentStorageModule = persistentStorageModule
        }

        fun build() = OasStubGuiceConfiguration(
            servletPrefix,
            adminServletPrefix,
            jettyServerSupplier,
            sessionStorageModule,
            persistentStorageModule
        )
    }
}