package io.github.ktakashi.oas.test.server.reactive

import io.github.ktakashi.oas.configuration.OasApplicationServletProperties
import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.test.OasStubTestProperties
import io.github.ktakashi.oas.test.OasStubTestService
import io.github.ktakashi.oas.web.reactive.OasStubApiHandler
import io.github.ktakashi.oas.web.reactive.RouterFunctionBuilder
import io.github.ktakashi.oas.web.reactive.RouterFunctionFactory
import jakarta.annotation.PostConstruct
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.SmartLifecycle
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.GenericApplicationContext
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter
import org.springframework.web.reactive.DispatcherHandler
import org.springframework.web.reactive.function.server.support.RouterFunctionMapping
import org.springframework.web.server.adapter.WebHttpHandlerBuilder
import reactor.netty.DisposableServer
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

@Configuration
@EnableConfigurationProperties(value = [OasStubReactiveServerProperties::class, OasStubTestProperties::class])
class OasStubReactiveConfiguration(private val servletProperties: OasApplicationServletProperties,
                                   val serverProperties: OasStubReactiveServerProperties,
                                   private val testProperties: OasStubTestProperties,
                                   private val applicationContext: ConfigurableApplicationContext,
                                   private val apiHandler: OasStubApiHandler,
                                   private val functionBuilders: Set<RouterFunctionBuilder>)
    : SmartLifecycle {
    private lateinit var httpServer: DisposableServer
    private lateinit var oasStubTestService: OasStubTestService

    fun nettyHttpServer(): DisposableServer {
        val routerFunction = RouterFunctionFactory(apiHandler).buildRouterFunction(servletProperties.prefix, functionBuilders)

        val newContext = applicationContext.also { context ->
            context.beanFactory.registerSingleton("oasStubRouterFunction", routerFunction)
//            val mapping = RouterFunctionMapping(routerFunction).apply {
//                applicationContext = context
//            }
//            context.beanFactory.registerSingleton("routerFunctionMapping", mapping)
//            context.refresh()
//            context.beanFactory.registerSingleton(WebHttpHandlerBuilder.WEB_HANDLER_BEAN_NAME, DispatcherHandler(context))
        }
        val handler = WebHttpHandlerBuilder.applicationContext(newContext).build()
        val adapter = ReactorHttpHandlerAdapter(handler)
        return HttpServer.create().port(serverProperties.port).handle(adapter).bindNow()
    }

    @PostConstruct
    fun init() {
        val beanFactory = applicationContext.beanFactory
        httpServer = nettyHttpServer()
        oasStubTestService = OasStubTestService(testProperties, beanFactory.getBean(ApiRegistrationService::class.java), beanFactory.getBean(ApiObserver::class.java))
        beanFactory.registerSingleton("oasStubTestService", oasStubTestService)
    }

    fun resetConfiguration() {
        oasStubTestService.clear()
        oasStubTestService.setup()
    }

    override fun start() {
        if (httpServer.isDisposed) {
            httpServer = nettyHttpServer()
        }
        resetConfiguration()
    }

    override fun stop() {
        oasStubTestService.clear()
        httpServer.disposeNow()
    }

    override fun isRunning(): Boolean = !httpServer.isDisposed
}