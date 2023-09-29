package io.github.ktakashi.oas.test

import java.util.Optional
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent
import org.springframework.context.ApplicationListener
import org.springframework.core.env.MapPropertySource
import org.springframework.test.context.TestContext
import org.springframework.test.context.support.AbstractTestExecutionListener
import org.springframework.test.context.support.TestPropertySourceUtils


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

private const val SERVER_PORT = "server.port"
class OasStubTestApplicationListener: ApplicationListener<ApplicationEnvironmentPreparedEvent> {
    override fun onApplicationEvent(event: ApplicationEnvironmentPreparedEvent) {
        val environment = event.environment
        val propertySource = environment.propertySources
        val serverPort = environment.getProperty(SERVER_PORT, Int::class.java)
        if (serverPort != null) {
            /*
             * This might not be a good idea as it hijacks the port allocation.
             * (I can't think of any harmful situation though, only limiting the range).
             */
            if (serverPort == 0 && propertySource.contains(TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME)) {
                val randomPort = findAvailableTcpPort(25000, 27500)
                val source = (propertySource[TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME] as MapPropertySource).source
                source[SERVER_PORT] = randomPort
            }
            if (!propertySource.contains(OasStubTestProperties.OAS_STUB_TEST_PROPERTY_PREFIX)) {
                val oasServerSource = mutableMapOf<String, Any>()
                oasServerSource["${OasStubTestProperties.OAS_STUB_TEST_PROPERTY_PREFIX}.server.port"] = environment.getProperty(SERVER_PORT)!!
                propertySource.addFirst(
                    MapPropertySource(
                        OasStubTestProperties.OAS_STUB_TEST_PROPERTY_PREFIX,
                        oasServerSource
                    ))
            }
        }
    }

}