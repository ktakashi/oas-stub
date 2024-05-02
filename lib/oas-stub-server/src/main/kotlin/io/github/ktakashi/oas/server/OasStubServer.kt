package io.github.ktakashi.oas.server

import io.github.ktakashi.oas.server.handlers.OasStubApiHandler
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
    private var httpServer: DisposableServer? = null
    private var httpsServer: DisposableServer? = null

    fun start() {
        if (!koinInitialized) {
            startKoin {
                modules(validatorModule)
                modules(makeEngineModule(options))
                modules(makeStorageModule(options.persistentStorage, options.sessionStorage))
            }
            oasStubApiHandler = OasStubApiHandler()
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

    private fun createNettyServer(port: Int): HttpServer = HttpServer.create().port(port).route { routes ->
        routes.route(prefix(options.stubPath), oasStubApiHandler)
    }

    private fun prefix(path: String): Predicate<in HttpServerRequest> =
        Predicate<HttpServerRequest> { request -> request.uri().startsWith(path) }
}

