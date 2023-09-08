package io.github.ktakashi.oas.test

import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.model.ApiConfiguration
import io.github.ktakashi.oas.model.ApiData
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.model.ApiDelay
import io.github.ktakashi.oas.model.ApiHeaders
import io.github.ktakashi.oas.model.ApiOptions
import java.util.*
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent
import org.springframework.context.ApplicationListener
import org.springframework.core.env.MapPropertySource
import org.springframework.core.io.Resource
import org.springframework.test.context.TestContext
import org.springframework.test.context.support.AbstractTestExecutionListener
import org.springframework.test.util.TestSocketUtils

class OasStubTestService(private val properties: OasStubTestProperties,
                         private val apiRegistrationService: ApiRegistrationService) {
    fun setup() {
        clear()
        properties.definitions.forEach { (k, v) ->
            apiRegistrationService.saveApiDefinitions(k, v.toApiDefinitions())
        }
    }

    fun clear() {
        apiRegistrationService.getAllNames().forEach { name ->
            apiRegistrationService.deleteApiDefinitions(name)
        }
    }

    fun createTestApiContext(name: String, script: Resource) = OasStubTestApiContext(apiRegistrationService, name, ApiDefinitions(script.inputStream.reader().readText()))

    fun getTestApiContext(name: String) = apiRegistrationService.getApiDefinitions(name).map { def ->
        OasStubTestApiContext(apiRegistrationService, name, def)
    }.orElseGet { OasStubTestApiContext(apiRegistrationService, name, ApiDefinitions()) }
}

class OasStubTestApiContext(private val apiRegistrationService: ApiRegistrationService,
                            private val name: String,
                            private val apiDefinitions: ApiDefinitions) {
    fun save() = apiRegistrationService.saveApiDefinitions(name, apiDefinitions)

    fun getApiConfiguration(path: String) = Optional.ofNullable(apiDefinitions.configurations?.get(path))

    fun updateApi(path: String, configuration: ApiConfiguration) =
            OasStubTestApiContext(apiRegistrationService, name, apiDefinitions.updateConfiguration(path, configuration))

    fun updateHeaders(headers: ApiHeaders) = OasStubTestApiContext(apiRegistrationService, name, apiDefinitions.updateHeaders(headers))

    fun updateData(data: Map<String, Any>) = OasStubTestApiContext(apiRegistrationService, name, apiDefinitions.updateData(ApiData(data)))

    fun updateDelay(delay: ApiDelay) = OasStubTestApiContext(apiRegistrationService, name, apiDefinitions.updateDelay(delay))

    fun updateOptions(options: ApiOptions) = OasStubTestApiContext(apiRegistrationService, name, apiDefinitions.updateOptions(options))

}

private const val PROPERTY_SOURCE_KEY = "oas.stub.test"

class OasStubTestExecutionListener: AbstractTestExecutionListener() {

    override fun beforeTestExecution(testContext: TestContext) {
        getTestService(testContext).ifPresent { service ->
            service.setup()
        }
    }

    override fun afterTestExecution(testContext: TestContext) {
        getTestService(testContext).ifPresent { service ->
            service.clear()
        }
    }

    private fun getTestService(testContext: TestContext): Optional<OasStubTestService> {
        return try {
            val context = testContext.applicationContext
            if (context.containsBean(OAS_STUB_TEST_SERVICE_NAME)) {
                Optional.of(context.getBean(OAS_STUB_TEST_SERVICE_NAME, OasStubTestService::class.java))
            } else {
                Optional.empty()
            }
        } catch (e: Exception) {
            Optional.empty()
        }
    }
}

class OasStubTestApplicationListener: ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    override fun onApplicationEvent(event: ApplicationEnvironmentPreparedEvent) {
        val environment = event.environment
        val propertySource = environment.propertySources
        val serverPort = environment.getProperty("server.port", Int::class.java)
        if (serverPort != null) {
            // let's replace the server port to random one here to deceive spring boot
            // NB: I hope the name `Inlined Test Properties` won't change in the future...
            if (serverPort == 0 && propertySource.contains("Inlined Test Properties")) {
                val randomPort = TestSocketUtils.findAvailableTcpPort()
                val source = (propertySource.get("Inlined Test Properties") as MapPropertySource).source
                source["server.port"] = randomPort
            }
            if (!propertySource.contains(PROPERTY_SOURCE_KEY)) {
                val oasServerSource = mutableMapOf<String, Any>()
                oasServerSource["${PROPERTY_SOURCE_KEY}.server.port"] = environment.getProperty("server.port")!!
                propertySource.addFirst(MapPropertySource(PROPERTY_SOURCE_KEY, oasServerSource))
            }
        }
    }

}