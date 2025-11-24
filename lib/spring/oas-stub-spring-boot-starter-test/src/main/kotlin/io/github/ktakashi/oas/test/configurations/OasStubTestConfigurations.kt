package io.github.ktakashi.oas.test.configurations

import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.engine.apis.record.ApiRecorder
import io.github.ktakashi.oas.server.OasStubServer
import io.github.ktakashi.oas.server.api.OasStubApiForwardingResolver
import io.github.ktakashi.oas.server.handlers.OasStubRoutesBuilder
import io.github.ktakashi.oas.storages.apis.PersistentStorage
import io.github.ktakashi.oas.storages.apis.SessionStorage
import io.github.ktakashi.oas.test.OasStubTestProperties
import io.github.ktakashi.oas.test.OasStubTestService
import io.github.ktakashi.oas.test.listeners.OAS_STUB_SERVER_CONFIGURATION_BEAN_NAME
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.SmartLifecycle
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.json.JsonMapper


@Configuration(OAS_STUB_SERVER_CONFIGURATION_BEAN_NAME)
@EnableConfigurationProperties(OasStubTestProperties::class)
@EnableAutoConfiguration
class OasStubServerConfiguration(internal val properties: OasStubTestProperties,
                                 private val jsonMapper: JsonMapper,
                                 private val oasStubRoutesBuilders: Set<OasStubRoutesBuilder>,
                                 private val oasStubApiForwardingResolvers: Set<OasStubApiForwardingResolver>): KoinComponent, SmartLifecycle {
    private lateinit var oasStubTestService: OasStubTestService
    private lateinit var oasStubServer: OasStubServer

    @Bean
    fun oasStubServer(sessionStorage: SessionStorage, persistentStorage: PersistentStorage): OasStubServer {
        oasStubServer = OasStubServer(properties.toOasStubOptions(jsonMapper, sessionStorage, persistentStorage, oasStubRoutesBuilders, oasStubApiForwardingResolvers))
        oasStubServer.init()
        return oasStubServer
    }

    @Bean
    fun oasStubTestService(oasStubServer: OasStubServer): OasStubTestService {
        oasStubTestService = OasStubTestService(properties,
            inject<ApiRegistrationService>().value,
            inject<ApiObserver>().value,
            inject<ApiRecorder>().value,
            inject<JsonMapper>().value)
        return oasStubTestService
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
