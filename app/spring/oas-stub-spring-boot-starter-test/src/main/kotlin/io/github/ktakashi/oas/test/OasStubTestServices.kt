package io.github.ktakashi.oas.test

import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.model.ApiDefinitions
import java.lang.Exception
import java.util.*
import org.springframework.boot.context.event.ApplicationPreparedEvent
import org.springframework.cloud.context.environment.EnvironmentChangeEvent
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.io.Resource
import org.springframework.test.context.TestContext
import org.springframework.test.context.support.AbstractTestExecutionListener

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

    fun create(name: String, script: Resource) {
        apiRegistrationService.saveApiDefinitions(name, ApiDefinitions(script.inputStream.reader().readText()))
    }
}

private const val PROPERTY_SOURCE_KEY = "oas.stub.test"

class OasStubTestExecutionListener: AbstractTestExecutionListener() {
    override fun beforeTestClass(testContext: TestContext) {
        // try to resolve ${local.server.port}
        val environment = testContext.applicationContext.environment as ConfigurableEnvironment
        val port = environment.getProperty("local.server.port", Int::class.java)
        if (port != null) {
            val propertySource = (environment.propertySources.get(PROPERTY_SOURCE_KEY) as MapPropertySource).source
            val key = "${PROPERTY_SOURCE_KEY}.server.port"
            propertySource[key] = port
            testContext.publishEvent {
                EnvironmentChangeEvent(setOf(key))
            }
        }
    }
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

class OasStubTestApplicationListener: ApplicationListener<ApplicationPreparedEvent> {
    override fun onApplicationEvent(event: ApplicationPreparedEvent) {
        val environment = event.applicationContext.environment as ConfigurableEnvironment
        val propertySource = environment.propertySources
        if (!propertySource.contains(PROPERTY_SOURCE_KEY)) {
            propertySource.addFirst(MapPropertySource(PROPERTY_SOURCE_KEY, mutableMapOf()))
        }
    }

}