package io.github.ktakashi.oas.guice.server

import com.google.inject.Injector
import com.google.inject.servlet.GuiceFilter
import com.google.inject.servlet.GuiceServletContextListener
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceResourceConfig
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceServerConfiguration
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import jakarta.servlet.DispatcherType
import java.util.EnumSet
import org.eclipse.jetty.ee10.servlet.ServletHolder
import org.eclipse.jetty.ee10.webapp.WebAppContext
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.SslConnectionFactory
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.glassfish.jersey.servlet.ServletContainer

/**
 * OAS Stub server.
 *
 * The server uses Jetty as its underlying server implementation.
 *
 * This server must be created via [OasStubGuiceInjectors.createServerInjector]
 */
@Named @Singleton
class OasStubServer
@Inject constructor(private val configuration: OasStubGuiceServerConfiguration,
                    private val injector: Injector) {
    private val server: Server = configuration.jettyServerSupplier.get()

    init {
        configure()
    }

    /**
     * Starts the server.
     */
    fun start() {
        server.start()
    }

    /**
     * Stop the server
     */
    fun stop() {
        server.stop()
    }

    /**
     * Check if the server is running
     */
    val isRunning
        get() = server.isRunning

    /**
     * Returns the port number of this server.
     *
     * @param n specifies the connector index, default `0`
     */
    @JvmOverloads
    fun port(n: Int = 0): Int = (server.connectors[n] as ServerConnector).localPort

    private fun configure() {
        configuration.serverConnectors.forEach { config ->
            val connectionFactories = if (config.httpConfiguration.securePort > 0) {
                arrayOf(SslConnectionFactory(config.sslContextFactorySupplier.get(), HttpVersion.HTTP_1_1.asString()), HttpConnectionFactory(config.httpConfiguration))
            } else {
                arrayOf(HttpConnectionFactory(config.httpConfiguration))
            }
            val connector = ServerConnector(server, *connectionFactories).apply {
                name = config.name
                host = config.host
                port = config.port
                isReusePort = true
            }
            server.addConnector(connector)
        }
        val holder = ServletHolder(ServletContainer::class.java).apply {
            setInitParameter("jakarta.ws.rs.Application", OasStubGuiceResourceConfig::class.java.name)
        }

        val webAppContext = WebAppContext().apply {
            this.server = this@OasStubServer.server
            setInitParameter("org.eclipse.jetty.servlet.Default.dirAllowed", "false")

            // To avoid static injection and context destruction, we retrieve single instance from injector
            val guiceFilter = injector.getInstance(GuiceFilter::class.java)
            addFilter(guiceFilter, "/*", EnumSet.allOf(DispatcherType::class.java))
            addServlet(holder, "${configuration.oasStubConfiguration.adminPrefix}/*")
            setBaseResourceAsString("/")
            contextPath = "/"
            addEventListener(object : GuiceServletContextListener() {
                override fun getInjector(): Injector = this@OasStubServer.injector
            })
            configuration.jettyWebAppContextCustomizers.forEach { customizer ->
                customizer.customize(injector, this)
            }
        }
        setHandler(server, webAppContext)
    }

    private tailrec fun setHandler(handlerWrapper: Handler.Wrapper, handlerToAdd: Handler) {
        when (val currentInnerHandler = handlerWrapper.handler) {
            null -> handlerWrapper.handler = handlerToAdd
            is Handler.Collection -> currentInnerHandler.addHandler(handlerToAdd)
            is Handler.Wrapper -> setHandler(currentInnerHandler, handlerToAdd)
            else -> {
                val handlerList = ContextHandlerCollection()
                handlerList.addHandler(currentInnerHandler)
                handlerList.addHandler(handlerToAdd)
                handlerWrapper.handler = handlerList
            }
        }
    }
}