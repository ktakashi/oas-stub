package io.github.ktakashi.oas.test.server.reactive

import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.reactive.configuration.AutoOasReactiveConfiguration
import io.github.ktakashi.oas.test.OasStubTestProperties
import io.github.ktakashi.oas.test.OasStubTestService
import io.netty.handler.ssl.util.SelfSignedCertificate
import jakarta.annotation.PostConstruct
import java.security.cert.X509Certificate
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.SmartLifecycle
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter
import org.springframework.web.server.adapter.WebHttpHandlerBuilder
import reactor.netty.DisposableServer
import reactor.netty.http.Http11SslContextSpec
import reactor.netty.http.server.HttpServer

internal const val OAS_STUB_REACTIVE_SERVER_PROPERTY_PREFIX = "${OasStubTestProperties.OAS_STUB_TEST_PROPERTY_PREFIX}.server"
internal const val OAS_STUB_REACTIVE_CONFIGURATION_BEAN_NAME = "oasStubReactiveConfiguration"


@ConfigurationProperties(OAS_STUB_REACTIVE_SERVER_PROPERTY_PREFIX)
data class OasStubReactiveServerProperties(
    val parallelism: Int = Runtime.getRuntime().availableProcessors(),
    val port: Int = 8080,
    val httpsPort: Int = -1,
    val resetConfigurationAfterEachTest: Boolean = false
)

@AutoConfiguration(after = [AutoOasReactiveConfiguration::class])
@EnableConfigurationProperties(OasStubTestProperties::class)
@Configuration
class AutoOasStubTestServiceConfiguration(private val testProperties: OasStubTestProperties) {
    @Bean
    @ConditionalOnMissingBean
    fun oasStubTestService(apiRegistrationService: ApiRegistrationService, apiObserver: ApiObserver) =
        OasStubTestService(testProperties, apiRegistrationService, apiObserver)

    @Bean
    @ConditionalOnMissingBean
    fun oasStubSelfSignedCertificate() = SelfSignedCertificate()

    @Bean
    @ConditionalOnMissingBean
    fun oasStubTestServerCertificate(certificate: SelfSignedCertificate): X509Certificate = certificate.cert()
}

@Configuration(OAS_STUB_REACTIVE_CONFIGURATION_BEAN_NAME)
@EnableConfigurationProperties(value = [OasStubReactiveServerProperties::class, OasStubTestProperties::class])
@EnableAutoConfiguration
class OasStubReactiveConfiguration(val serverProperties: OasStubReactiveServerProperties,
                                   private val applicationContext: ConfigurableApplicationContext,
                                   private val certificate: SelfSignedCertificate)
    : SmartLifecycle {
    companion object {
        const val OAS_STUB_SERVER_CERTIFICATE_BEAN_NAME = "oasStubTestServerCertificate"
    }

    private lateinit var httpServer: DisposableServer
    private var httpsServer: DisposableServer? = null
    private lateinit var oasStubTestService: OasStubTestService

    fun nettyHttpServer(): Pair<DisposableServer, DisposableServer?> {
        val newContext = applicationContext
        // newContext.beanFactory.registerSingleton("oasStubRouterFunction", routerFunction)
        // val mapping = RouterFunctionMapping(routerFunction).apply { applicationContext = newContext }
        // newContext.beanFactory.registerSingleton("routerFunctionMapping", mapping)
        // newContext.refresh()
        // newContext.beanFactory.registerSingleton(WebHttpHandlerBuilder.WEB_HANDLER_BEAN_NAME, DispatcherHandler(newContext))
        val handler = WebHttpHandlerBuilder.applicationContext(newContext).build()
        val adapter = ReactorHttpHandlerAdapter(handler)
        return HttpServer.create().port(serverProperties.port).handle(adapter).bindNow() to
         if (serverProperties.httpsPort != -1) {
             val spec = Http11SslContextSpec.forServer(certificate.certificate(), certificate.privateKey())

             HttpServer.create().port(serverProperties.httpsPort)
                 .secure { contextSpec -> contextSpec.sslContext(spec) }
                 .handle(adapter).bindNow()
         } else {
             null
         }

    }

    @PostConstruct
    fun init() {
        val beanFactory = applicationContext.beanFactory
        val (http, https) = nettyHttpServer()
        httpServer = http
        httpsServer = https
        oasStubTestService = beanFactory.getBean(OasStubTestService::class.java)
    }

    fun resetConfiguration() {
        oasStubTestService.clear()
        oasStubTestService.setup()
    }

    override fun start() {
        if (httpServer.isDisposed) {
            val (http, https) = nettyHttpServer()
            httpServer = http
            if (httpsServer?.isDisposed != true) {
                httpsServer?.disposeNow()
            }
            httpsServer = https
        }
        resetConfiguration()
    }

    override fun stop() {
        oasStubTestService.clear()
        httpServer.disposeNow()
        httpsServer?.disposeNow()
    }

    override fun isRunning(): Boolean = !httpServer.isDisposed
}