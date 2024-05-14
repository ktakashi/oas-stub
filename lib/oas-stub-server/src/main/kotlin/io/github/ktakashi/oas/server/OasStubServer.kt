package io.github.ktakashi.oas.server

import io.github.ktakashi.oas.server.handlers.OasStubAdminRoutesBuilder
import io.github.ktakashi.oas.server.handlers.OasStubApiHandler
import io.github.ktakashi.oas.server.handlers.OasStubMetricsRoutesBuilder
import io.github.ktakashi.oas.server.handlers.OasStubRoutes
import io.github.ktakashi.oas.server.modules.makeEngineModule
import io.github.ktakashi.oas.server.modules.makeStorageModule
import io.github.ktakashi.oas.server.modules.validatorModule
import io.github.ktakashi.oas.server.options.OasStubOptions
import io.netty.handler.ssl.util.SelfSignedCertificate
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.function.Predicate
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import reactor.netty.DisposableServer
import reactor.netty.http.Http2SslContextSpec
import reactor.netty.http.server.HttpServer
import reactor.netty.http.server.HttpServerRequest

/**
 * OAS Stub server.
 *
 * The simplest server can be created and started like this.
 *
 * ```Kotlin
 * OasStubServer(OasStubOptions.builder().build()).start()
 * ```
 *
 * See [OasStubOptions] for the more options
 */
class OasStubServer(private val options: OasStubOptions) {
    companion object {
        private var koinInitialized: Boolean = false
        @JvmStatic
        private val selfSignedCertificate = SelfSignedCertificate()
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
    private var certificate: X509Certificate? = null
    private var privateKey: PrivateKey? = null

    /**
     * Initializes the server.
     *
     * It won't be bound to any ports after this method call.
     * And this method is not needed to be called before [start].
     */
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
            val cert = serverOptions.ssl?.let { ssl ->
                if (ssl.keyAlias != null && ssl.keyPassword != null) {
                    ssl.keyStore?.getKey(ssl.keyAlias, ssl.keyPassword.toCharArray())?.let { key ->
                        ssl.keyStore.getCertificate(ssl.keyAlias)?.let { certificate ->
                            certificate as X509Certificate to key as PrivateKey
                        }
                    }
                } else {
                    null
                }
            } ?: selfSignedCertificate.let {
                it.cert() to it.key()
            }
            certificate = cert.first
            privateKey = cert.second
            initialized = true
        }
    }

    /**
     * Starts the server
     */
    fun start() {
        init()
        httpServer = createNettyServer(serverOptions.port).bindNow()
        if (serverOptions.httpsPort >= 0) {
            httpsServer = createNettyServer(serverOptions.httpsPort)
                .secure { spec -> spec.sslContext(Http2SslContextSpec.forServer(privateKey!!, certificate)) }
                .bindNow()
        }
    }

    /**
     * Stops the server
     */
    fun stop() {
        httpServer?.disposeNow()
        httpsServer?.disposeNow()
        httpServer = null
        httpsServer = null
        stopKoin()
        koinInitialized = false
        initialized = false
    }

    /**
     * Returns port number.
     *
     * If the server is not started, then `-1`
     */
    fun port() = httpServer?.port() ?: -1
    /**
     * Returns HTTPS port number.
     *
     * If the server is not started or HTTPS is not configured, then `-1`
     */
    fun httpsPort() = httpsServer?.port() ?: -1

    /***
     * Returns `true` if the server is running.
     */
    fun isRunning() = httpServer != null || httpsServer != null

    /**
     * Returns stub path of the server
     */
    fun stubPath() = stubOptions.stubPath

    /**
     * Checks if the admin endpoints are enabled or not.
     */
    fun adminEnabled() = stubOptions.enableAdmin

    /**
     * Returns admin path of the server
     */
    fun adminPath() = stubOptions.adminPath

    /**
     * Checks if the metrics endpoints are enabled or not
     */
    fun metricsEnabled() = stubOptions.enableMetrics

    /**
     * Returns metrics path of the server
     */
    fun metricsPath() = "${stubOptions.adminPath}${stubOptions.metricsPath}"

    /**
     * Returns TLS certificate of the server, if configured
     */
    fun certificate() = certificate

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

