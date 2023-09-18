package io.github.ktakashi.oas.guice.configurations

import java.util.function.Supplier
import org.eclipse.jetty.ee10.webapp.WebAppContext
import org.eclipse.jetty.server.Server

fun interface JettyServerSupplier: Supplier<Server>

val defaultJettyServerSupplier: JettyServerSupplier = JettyServerSupplier { Server() }

fun interface JettyWebAppContextCustomizer {
    fun customize(webAppContext: WebAppContext)
}