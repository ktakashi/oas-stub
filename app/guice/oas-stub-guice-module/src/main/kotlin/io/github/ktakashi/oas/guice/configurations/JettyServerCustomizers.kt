package io.github.ktakashi.oas.guice.configurations

import com.google.inject.Injector
import java.util.function.Supplier
import org.eclipse.jetty.ee10.servlet.ServletHolder
import org.eclipse.jetty.ee10.webapp.WebAppContext
import org.eclipse.jetty.server.Server
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.servlet.ServletContainer
import org.glassfish.jersey.servlet.ServletProperties

fun interface JettyServerSupplier: Supplier<Server>

val defaultJettyServerSupplier: JettyServerSupplier = JettyServerSupplier { Server() }

fun interface JettyWebAppContextCustomizer {
    fun customize(injector: Injector, webAppContext: WebAppContext)
}

object OasStubServerUtil {
    @JvmStatic
    fun <T: ResourceConfig> resourceConfigServletCustomizer(path: String, resourceConfigClass: Class<T>) = JettyWebAppContextCustomizer { _, webAppContext ->
        val holder = ServletHolder(ServletContainer::class.java).apply {
            setInitParameter("jakarta.ws.rs.Application", resourceConfigClass.name)
        }
        webAppContext.addServlet(holder, path)
    }
}