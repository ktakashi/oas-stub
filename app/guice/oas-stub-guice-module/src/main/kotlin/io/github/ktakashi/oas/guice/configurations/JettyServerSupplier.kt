package io.github.ktakashi.oas.guice.configurations

import java.util.function.Supplier
import org.eclipse.jetty.server.Server

interface JettyServerSupplier: Supplier<Server>

val defaultJettyServerSupplier: JettyServerSupplier = object: JettyServerSupplier {
    override fun get(): Server = Server()
}