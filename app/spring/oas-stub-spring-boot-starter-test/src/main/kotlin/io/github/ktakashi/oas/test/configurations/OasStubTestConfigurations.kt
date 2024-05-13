package io.github.ktakashi.oas.test.configurations

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.server.OasStubServer
import io.github.ktakashi.oas.server.handlers.OasStubRoutesBuilder
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import io.github.ktakashi.oas.test.OAS_STUB_SERVER_BEAN_NAME
import io.github.ktakashi.oas.test.OAS_STUB_TEST_SERVICE_BEAN_NAME
import io.github.ktakashi.oas.test.OasStubTestProperties
import io.github.ktakashi.oas.test.OasStubTestService
import io.github.ktakashi.oas.test.listeners.OAS_STUB_SERVER_CONFIGURATION_BEAN_NAME
import jakarta.annotation.PostConstruct
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.SmartLifecycle
import org.springframework.context.annotation.Configuration


@Configuration(OAS_STUB_SERVER_CONFIGURATION_BEAN_NAME)
@EnableConfigurationProperties(OasStubTestProperties::class)
@EnableAutoConfiguration
class OasStubServerConfiguration(internal val properties: OasStubTestProperties,
                                 private val applicationContext: ConfigurableApplicationContext,
                                 private val objectMapper: ObjectMapper,
                                 private val sessionStorage: SessionStorage,
                                 private val persistentStorage: PersistentStorage,
                                 private val oasStubRoutesBuilders: Set<OasStubRoutesBuilder>): KoinComponent, SmartLifecycle {
    private lateinit var oasStubTestService: OasStubTestService
    private lateinit var oasStubServer: OasStubServer

    @PostConstruct
    fun init() {
        val beanFactory = applicationContext.beanFactory
        if (!beanFactory.containsBean(OAS_STUB_SERVER_BEAN_NAME)) {
            oasStubServer = OasStubServer(properties.toOasStubOptions(objectMapper, sessionStorage, persistentStorage, oasStubRoutesBuilders))
            oasStubServer.init()
            beanFactory.registerSingleton(OAS_STUB_SERVER_BEAN_NAME, oasStubServer)
        } else {
            oasStubServer = beanFactory.getBean(OasStubServer::class.java)
        }
        if (!beanFactory.containsBean(OAS_STUB_TEST_SERVICE_BEAN_NAME)) {
            oasStubTestService = OasStubTestService(properties, inject<ApiRegistrationService>().value, inject<ApiObserver>().value)
            beanFactory.registerSingleton(OAS_STUB_TEST_SERVICE_BEAN_NAME, oasStubTestService)
        } else {
            oasStubTestService = beanFactory.getBean(OasStubTestService::class.java)
        }
    }

    override fun start() {
        if (!oasStubServer.isRunning()) {
            oasStubServer.start()
        }
        oasStubTestService.setup()
    }

    override fun stop() {
        if (oasStubServer.isRunning()) {
            oasStubServer.stop()
        }
    }

    override fun isRunning(): Boolean = oasStubServer.isRunning()

    fun resetConfiguration() {
        oasStubTestService.clear()
        oasStubTestService.setup()
    }
}
