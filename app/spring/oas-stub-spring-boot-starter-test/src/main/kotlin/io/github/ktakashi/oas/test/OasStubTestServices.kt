package io.github.ktakashi.oas.test

import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.model.ApiDefinitions
import java.lang.Exception
import java.util.*
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