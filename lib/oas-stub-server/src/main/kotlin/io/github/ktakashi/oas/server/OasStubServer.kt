package io.github.ktakashi.oas.server

import io.github.ktakashi.oas.server.handlers.OasStubAdminRoutesBuilder
import io.github.ktakashi.oas.server.handlers.OasStubApiHandler
import io.github.ktakashi.oas.server.handlers.OasStubMetricsRoutesBuilder
import io.github.ktakashi.oas.server.handlers.OasStubRoutes
import io.github.ktakashi.oas.server.modules.makeEngineModule
import io.github.ktakashi.oas.server.modules.makeStorageModule
import io.github.ktakashi.oas.server.modules.validatorModule
import io.github.ktakashi.oas.server.options.OasStubOptions
import java.util.function.Predicate
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import reactor.netty.DisposableServer
import reactor.netty.http.server.HttpServer
import reactor.netty.http.server.HttpServerRequest

class OasStubServer(private val options: OasStubOptions) {
    companion object {
        private var koinInitialized: Boolean = false
    }
    private var initialized = false
    private lateinit var oasStubApiHandler: OasStubApiHandler
    private lateinit var oasStubAdminRoutesBuilder: OasStubAdminRoutesBuilder
    private lateinit var oasStubMetricsRoutesBuilder: OasStubMetricsRoutesBuilder
    private lateinit var koin: Koin
    private var httpServer: DisposableServer? = null
    private var httpsServer: DisposableServer? = null
    private val serverOptions = options.serverOptions
    private val stubOptions = options.stubOptions

    fun init() {
        if (!initialized) {
            if (!koinInitialized) {
                startKoin {
                    modules(validatorModule)
                    modules(makeEngineModule(stubOptions))
                    modules(makeStorageModule(stubOptions.persistentStorage, stubOptions.sessionStorage))
                    this@OasStubServer.koin = koin
                }
                koinInitialized = true
            } else {
                koin = GlobalContext.get()
            }
            oasStubApiHandler = OasStubApiHandler()
            oasStubAdminRoutesBuilder = OasStubAdminRoutesBuilder(options.stubOptions)
            oasStubMetricsRoutesBuilder = OasStubMetricsRoutesBuilder(options.stubOptions)
            initialized = true
        }
    }

    fun start() {
        init()
        httpServer = createNettyServer(serverOptions.port).bindNow()
        if (serverOptions.httpsPort >= 0) {
            httpsServer = createNettyServer(serverOptions.httpsPort).bindNow()
        }
    }

    fun stop() {
        httpServer?.disposeNow()
        httpsServer?.disposeNow()
        httpServer = null
        httpsServer = null
    }

    fun port() = httpServer?.port()
    fun httpsPort() = httpsServer?.port()
    fun isRunning() = httpServer != null || httpsServer != null
    fun stubPath() = stubOptions.stubPath
    fun adminEnabled() = stubOptions.enableAdmin
    fun adminPath() = stubOptions.adminPath
    fun metricsEnabled() = stubOptions.enableMetrics
    fun metricsPath() = stubOptions.metricsPath

    private fun createNettyServer(port: Int): HttpServer = HttpServer.create().port(port)
        .accessLog(serverOptions.enableAccessLog)
        .route { routes ->
            val oasStubRoutes = OasStubRoutes(routes, koin)
            oasStubAdminRoutesBuilder.build(oasStubRoutes)
            oasStubMetricsRoutesBuilder.build(oasStubRoutes)
            stubOptions.routesBuilders.forEach { builder -> builder.build(oasStubRoutes) }
            routes.route(prefix(stubOptions.stubPath), oasStubApiHandler)
        }

    private fun prefix(path: String): Predicate<in HttpServerRequest> =
        Predicate<HttpServerRequest> { request -> request.uri().startsWith(path) }
}

