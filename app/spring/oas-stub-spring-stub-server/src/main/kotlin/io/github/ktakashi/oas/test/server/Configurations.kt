package io.github.ktakashi.oas.test.server

import com.google.inject.Injector
import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.guice.configurations.JettyWebAppContextCustomizer
import io.github.ktakashi.oas.guice.configurations.OasStubConfiguration
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceServerConfiguration
import io.github.ktakashi.oas.guice.configurations.OasStubServerConnectorConfiguration
import io.github.ktakashi.oas.guice.injector.createServerInjector
import io.github.ktakashi.oas.guice.server.OasStubServer
import io.github.ktakashi.oas.test.OAS_STUB_SERVER_BEAN_NAME
import io.github.ktakashi.oas.test.OAS_STUB_TEST_SERVICE_BEAN_NAME
import io.github.ktakashi.oas.test.OasStubTestProperties
import io.github.ktakashi.oas.test.OasStubTestService
import jakarta.annotation.PostConstruct
import org.eclipse.jetty.server.HttpConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.SmartLifecycle
import org.springframework.context.annotation.Configuration

internal const val OAS_STUB_SERVER_PROPERTY_PREFIX = "${OasStubTestProperties.OAS_STUB_TEST_PROPERTY_PREFIX}.server"
internal const val OAS_STUB_SERVER_CONFIGURATION_BEAN_NAME = "oasStubServerConfiguration"

@Configuration(OAS_STUB_SERVER_CONFIGURATION_BEAN_NAME)
@EnableConfigurationProperties(value = [OasStubServerProperties::class, OasStubTestProperties::class])
class OasStubServerConfiguration(internal val serverProperties: OasStubServerProperties,
                                 private val testProperties: OasStubTestProperties,
                                 private val jettyWebAppContextCustomizers: Set<JettyWebAppContextCustomizer>,
                                 private val beanFactory: DefaultListableBeanFactory,
                                 @Autowired(required = false) private val sslContextFactorySupplier: OasStubServerConnectorConfiguration.SslContextFactorySupplier?)
    : SmartLifecycle {

    private lateinit var injector: Injector
    private lateinit var oasStubServer: OasStubServer
    private lateinit var oasStubTestService: OasStubTestService

    @PostConstruct
    fun init() {
        injector = createServerInjector(OasStubGuiceServerConfiguration.builder()
            .serverConnectors(serverProperties.toServerConnectors(sslContextFactorySupplier))
            .oasStubConfiguration(serverProperties.toOasStubConfiguration())
            .jettyWebAppContextCustomizers(jettyWebAppContextCustomizers)
            .build())
        oasStubServer = injector.getInstance(OasStubServer::class.java)
        oasStubTestService = OasStubTestService(testProperties, injector.getInstance(ApiRegistrationService::class.java), injector.getInstance(ApiObserver::class.java))

        updateBean(OAS_STUB_SERVER_BEAN_NAME, oasStubServer)
        updateBean(OAS_STUB_TEST_SERVICE_BEAN_NAME, oasStubTestService)
    }

    private fun updateBean(name: String, bean: Any) {
        if (beanFactory.containsBean(name)) {
            beanFactory.destroySingleton(name)
        }
        beanFactory.registerSingleton(name, bean)
    }

    override fun start() {
        if (isRunning) {
            oasStubTestService.clear()
            oasStubTestService.setup()
            return
        }
        oasStubServer.start()
        oasStubTestService.setup()
    }

    override fun stop() {
        oasStubTestService.clear()
        oasStubServer.stop()
    }

    override fun isRunning() = oasStubServer.isRunning

    fun resetConfiguration() {
        oasStubTestService.clear()
        oasStubTestService.setup()
    }
}

@ConfigurationProperties(OAS_STUB_SERVER_PROPERTY_PREFIX)
data class OasStubServerProperties(
    val servletPrefix: String = "/oas",
    val adminPrefix: String = "/__admin",
    val parallelism: Int = Runtime.getRuntime().availableProcessors(),
    val port: Int = 8080,
    val httpsPort: Int = -1,
    val resetConfigurationAfterEachTest: Boolean = false
) {
    fun toServerConnectors(sslContextFactorySupplier: OasStubServerConnectorConfiguration.SslContextFactorySupplier?): List<OasStubServerConnectorConfiguration> {
        return if (httpsPort < 0) {
            listOf(OasStubServerConnectorConfiguration(name = "http", port = port))
        } else {
            listOf(OasStubServerConnectorConfiguration(name = "http", port = port),
                OasStubServerConnectorConfiguration(name = "https", port = httpsPort, httpConfiguration = HttpConfiguration().apply {
                    securePort = httpsPort
                    secureScheme = "https"
                }, sslContextFactorySupplier = sslContextFactorySupplier ?: OasStubServerConnectorConfiguration.DEFAULT_SSL_CONTEXT_SUPPLIER))
        }
    }

    fun toOasStubConfiguration() = OasStubConfiguration(servletPrefix, adminPrefix, parallelism)
}