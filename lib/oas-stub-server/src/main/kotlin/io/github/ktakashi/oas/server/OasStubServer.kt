package io.github.ktakashi.oas.server

import io.github.ktakashi.oas.server.handlers.OasStubAdminRoutesBuilder
import io.github.ktakashi.oas.server.handlers.OasStubApiHandler
import io.github.ktakashi.oas.server.handlers.OasStubMetricsRoutesBuilder
import io.github.ktakashi.oas.server.modules.makeEngineModule
import io.github.ktakashi.oas.server.modules.makeStorageModule
import io.github.ktakashi.oas.server.modules.validatorModule
import io.github.ktakashi.oas.server.options.OasStubServerOptions
import java.util.function.Predicate
import org.koin.core.context.startKoin
import reactor.netty.DisposableServer
import reactor.netty.http.server.HttpServer
import reactor.netty.http.server.HttpServerRequest

class OasStubServer(private val options: OasStubServerOptions) {
    private var koinInitialized: Boolean = false
    private lateinit var oasStubApiHandler: OasStubApiHandler
    private lateinit var oasStubAdminRoutesBuilder: OasStubAdminRoutesBuilder
    private lateinit var oasStubMetricsRoutesBuilder: OasStubMetricsRoutesBuilder
    private var httpServer: DisposableServer? = null
    private var httpsServer: DisposableServer? = null
    private val stubOptions = options.stubOptions

    fun start() {
        if (!koinInitialized) {
            startKoin {
                modules(validatorModule)
                modules(makeEngineModule(stubOptions))
                modules(makeStorageModule(options.stubOptions.persistentStorage, options.stubOptions.sessionStorage))
            }
            oasStubApiHandler = OasStubApiHandler()
            oasStubAdminRoutesBuilder = OasStubAdminRoutesBuilder(options)
            oasStubMetricsRoutesBuilder = OasStubMetricsRoutesBuilder(options)
            koinInitialized = true
        }
        httpServer = createNettyServer(options.port).bindNow()
        if (options.httpsPort >= 0) {
            httpsServer = createNettyServer(options.httpsPort).bindNow()
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
    fun stubPath() = stubOptions.stubPath
    fun adminEnabled() = stubOptions.enableAdmin
    fun adminPath() = stubOptions.adminPath
    fun metricsEnabled() = stubOptions.enableMetrics
    fun metricsPath() = stubOptions.metricsPath

    private fun createNettyServer(port: Int): HttpServer = HttpServer.create().port(port)
        .accessLog(options.enableAccessLog)
        .route { routes ->
            oasStubAdminRoutesBuilder.build(routes)
            oasStubMetricsRoutesBuilder.build(routes)

            routes.route(prefix(stubOptions.stubPath), oasStubApiHandler)
        }

    private fun prefix(path: String): Predicate<in HttpServerRequest> =
        Predicate<HttpServerRequest> { request -> request.uri().startsWith(path) }
}

