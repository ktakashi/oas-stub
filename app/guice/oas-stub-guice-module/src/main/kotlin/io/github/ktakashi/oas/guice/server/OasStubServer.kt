package io.github.ktakashi.oas.guice.server

import io.github.ktakashi.oas.guice.configurations.JettyServerSupplier
import org.eclipse.jetty.server.Server

class OasStubServer(jettyServerSupplier: JettyServerSupplier) {
    private val server: Server = jettyServerSupplier.get()

}