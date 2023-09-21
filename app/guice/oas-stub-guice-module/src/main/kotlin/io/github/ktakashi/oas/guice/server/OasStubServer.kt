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
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.glassfish.jersey.servlet.ServletContainer

@Named @Singleton
class OasStubServer
@Inject constructor(private val configuration: OasStubGuiceServerConfiguration,
                    private val injector: Injector) {
    private val server: Server = configuration.jettyServerSupplier.get()

    init {
        configure()
    }

    fun start() {
        server.start()
    }

    fun stop() {
        server.stop()
    }

    @JvmOverloads
    fun port(n: Int = 0): Int = (server.connectors[n] as ServerConnector).localPort

    private fun configure() {
        configuration.serverConnectors.forEach { config ->
            val connector = ServerConnector(server).apply {
                name = config.name
                host = config.host
                port = config.port
                connectionFactories = listOf(HttpConnectionFactory(config.httpConfiguration))
            }
            server.addConnector(connector)
        }
        val holder = ServletHolder(ServletContainer::class.java).apply {
            setInitParameter("jakarta.ws.rs.Application", OasStubGuiceResourceConfig::class.java.name)
        }

        val webAppContext = WebAppContext().apply {
            this.server = this@OasStubServer.server
            addFilter(GuiceFilter::class.java, "/*", EnumSet.allOf(DispatcherType::class.java))
            addServlet(holder, "${configuration.oasStubConfiguration.adminPrefix}/*")
            setBaseResourceAsString("/")
            contextPath = "/"
            addEventListener(object: GuiceServletContextListener() {
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