package io.github.ktakashi.oas.server

import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.engine.apis.record.ApiRecorder
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.server.config.OasStubStaticConfigParser
import io.github.ktakashi.oas.server.handlers.OasStubAdminRoutesBuilder
import io.github.ktakashi.oas.server.handlers.OasStubApiHandler
import io.github.ktakashi.oas.server.handlers.OasStubForwardingApiHandler
import io.github.ktakashi.oas.server.handlers.OasStubMetricsRoutesBuilder
import io.github.ktakashi.oas.server.handlers.OasStubRecordsRoutesBuilder
import io.github.ktakashi.oas.server.handlers.OasStubRoutes
import io.github.ktakashi.oas.server.handlers.OasStubRoutesBuilder
import io.github.ktakashi.oas.server.modules.makeApiDefinitionMergerModule
import io.github.ktakashi.oas.server.modules.makeEngineModule
import io.github.ktakashi.oas.server.modules.makeStorageModule
import io.github.ktakashi.oas.server.modules.validatorModule
import io.github.ktakashi.oas.server.options.OasStubOptions
import io.github.ktakashi.oas.server.options.OasStubStubOptions
import io.netty.handler.codec.http2.Http2SecurityUtil
import io.netty.handler.ssl.ApplicationProtocolConfig
import io.netty.handler.ssl.ApplicationProtocolNames
import io.netty.handler.ssl.ClientAuth
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.SslProvider
import io.netty.handler.ssl.SupportedCipherSuiteFilter
import io.netty.handler.ssl.util.SelfSignedCertificate
import java.net.URI
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.function.Consumer
import java.util.function.Predicate
import org.koin.core.Koin
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import reactor.netty.DisposableServer
import reactor.netty.http.server.HttpServer
import reactor.netty.http.server.HttpServerRequest
import reactor.netty.tcp.AbstractProtocolSslContextSpec
import reactor.netty.tcp.SslProvider as TcpSslProvider

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
class OasStubServer(options: OasStubOptions) {
    companion object {
        private var koinInitialized: Boolean = false
        @JvmStatic
        private val selfSignedCertificate = SelfSignedCertificate()
    }
    private var initialized = false
    private val builderCreators: Set<(OasStubStubOptions) -> OasStubRoutesBuilder> = setOf(::OasStubAdminRoutesBuilder, ::OasStubMetricsRoutesBuilder, ::OasStubRecordsRoutesBuilder)
    private lateinit var apiRegistrationService: ApiRegistrationService
    private lateinit var apiObserver: ApiObserver
    private lateinit var apiRecorder: ApiRecorder
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
                    modules(makeApiDefinitionMergerModule(stubOptions))
                    this@OasStubServer.koin = koin
                }
                koinInitialized = true
            } else {
                koin = GlobalContext.get()
            }
            apiRegistrationService = koin.get<ApiRegistrationService>()
            apiObserver = koin.get<ApiObserver>()
            apiRecorder = koin.get<ApiRecorder>()
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
            reloadStaticStub()
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
                .secure { spec -> spec.sslContext(SslContextSpec(SslContextBuilder.forServer(privateKey!!, certificate)
                    .also { builder ->
                        serverOptions.ssl?.let { ssl ->
                            ssl.trustStore?.let { trustStore ->
                                builder.trustManager(trustStore.aliases().asSequence().mapNotNull { alias ->
                                    trustStore.getCertificate(alias) as X509Certificate
                                }.toList())
                            }
                            builder.clientAuth(ClientAuth.valueOf(ssl.clientAuth.name))
                        }
                    }) as TcpSslProvider.GenericSslContextSpec<*>)
                }.bindNow()
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
     * Register a stub definition
     *
     * [name] the name of the stub
     * [definitions] the definition
     */
    fun registerStub(name: String, definitions: ApiDefinitions) {
        apiRegistrationService.saveApiDefinitions(name, definitions).subscribe()
    }

    /**
     * Deregister stub of [name]
     */
    fun deregisterStub(name: String) {
        apiRegistrationService.deleteApiDefinitions(name).subscribe()
    }

    /**
     * Reload static stub
     */
    fun reloadStaticStub() {
        stubOptions.staticConfigurations.map { location ->
            OasStubStaticConfigParser.parse(URI.create(location))
        }.forEach { config ->
            config.forEach { (context, definition) ->
                registerStub(context, definition)
            }
        }
    }

    /**
     * Clear all metrics
     */
    fun resetMetrics() {
        apiObserver.clearApiMetrics()
    }

    /**
     * Clear all recordings
     */
    fun resetRecords() {
        apiRecorder.clearAllApiRecords()
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
     * Check if the records endpoints are enabled or not
     */
    fun recordsEnabled() = stubOptions.enableRecord

    /**
     * Returns records path of the server
     */
    fun recordsPath() = "${stubOptions.adminPath}${stubOptions.recordsPath}"

    /**
     * Returns TLS certificate of the server, if configured
     */
    fun certificate() = certificate

    private fun createNettyServer(port: Int): HttpServer = HttpServer.create().port(port)
        .accessLog(serverOptions.enableAccessLog)
        .route { routes ->
            val oasStubRoutes = OasStubRoutes(routes, koin)
            builderCreators.forEach { creator ->
                creator(stubOptions).build(oasStubRoutes)
            }
            stubOptions.routesBuilders.forEach { builder -> builder.build(oasStubRoutes) }
            routes.route(prefix(stubOptions.stubPath), OasStubApiHandler())
            if (stubOptions.forwardingResolvers.isNotEmpty()) {
                routes.route(prefix(stubOptions.forwardingPath), OasStubForwardingApiHandler(stubOptions))
            }
        }

    private fun prefix(path: String): Predicate<in HttpServerRequest> =
        Predicate<HttpServerRequest> { request -> request.uri().startsWith(path) }

    private class SslContextSpec(sslContextBuilder: SslContextBuilder): AbstractProtocolSslContextSpec<SslContextSpec>(sslContextBuilder) {
        companion object {
            internal val DEFAULT_CONFIGURATOR: Consumer<SslContextBuilder> =
                Consumer<SslContextBuilder> { sslCtxBuilder: SslContextBuilder ->
                    sslCtxBuilder.sslProvider(if (SslProvider.isAlpnSupported(SslProvider.OPENSSL)) SslProvider.OPENSSL else SslProvider.JDK)
                        .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                        .applicationProtocolConfig(
                            ApplicationProtocolConfig(
                                ApplicationProtocolConfig.Protocol.ALPN,
                                ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                                ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                                ApplicationProtocolNames.HTTP_2,
                                ApplicationProtocolNames.HTTP_1_1
                            )
                        )
                }
        }
        override fun get(): SslContextSpec = this

        override fun defaultConfiguration(): Consumer<SslContextBuilder> = DEFAULT_CONFIGURATOR

    }
}

