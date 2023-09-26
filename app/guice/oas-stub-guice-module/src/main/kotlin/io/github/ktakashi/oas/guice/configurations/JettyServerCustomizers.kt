package io.github.ktakashi.oas.guice.configurations

import com.google.inject.Injector
import java.util.function.Supplier
import org.eclipse.jetty.ee10.servlet.ServletHolder
import org.eclipse.jetty.ee10.webapp.WebAppContext
import org.eclipse.jetty.server.Server
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.servlet.ServletContainer

/**
 * Jetty server instance supplier interface
 */
fun interface JettyServerSupplier: Supplier<Server> {
    companion object {
        /**
         * Default Jetty server instance supplier
         */
        @JvmField
        val DEFAULT_JETTY_SERVER_SUPPLIER: JettyServerSupplier = JettyServerSupplier { Server() }
    }
}

/**
 * Jetty [WebAppContext] customizer.
 */
fun interface JettyWebAppContextCustomizer {
    /**
     * Customize the given [webAppContext]
     *
     * @param injector Guice injector where the target server is located
     * @param webAppContext The customization target
     */
    fun customize(injector: Injector, webAppContext: WebAppContext)
}

/**
 * OasStubServer utilities
 */
object OasStubServerUtil {
    /**
     * Creates a [JettyWebAppContextCustomizer] which associates the given [resourceConfigClass] to [path].
     */
    @JvmStatic
    fun <T: ResourceConfig> resourceConfigServletCustomizer(path: String, resourceConfigClass: Class<T>) = JettyWebAppContextCustomizer { _, webAppContext ->
        val holder = ServletHolder(ServletContainer::class.java).apply {
            setInitParameter("jakarta.ws.rs.Application", resourceConfigClass.name)
        }
        webAppContext.addServlet(holder, path)
    }
}