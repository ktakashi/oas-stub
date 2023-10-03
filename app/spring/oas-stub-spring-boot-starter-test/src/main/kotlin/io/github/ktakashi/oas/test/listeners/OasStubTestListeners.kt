package io.github.ktakashi.oas.test.listeners

import io.github.ktakashi.oas.test.AutoConfigureOasStub
import io.github.ktakashi.oas.test.OAS_STUB_TEST_SERVICE_BEAN_NAME
import io.github.ktakashi.oas.test.OasStubTestProperties
import io.github.ktakashi.oas.test.OasStubTestService
import io.github.ktakashi.oas.test.isPortShared
import io.github.ktakashi.oas.test.randomTcpPort
import java.util.Optional
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent
import org.springframework.context.ApplicationListener
import org.springframework.core.env.MapPropertySource
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestContextAnnotationUtils
import org.springframework.test.context.support.AbstractTestExecutionListener
import org.springframework.test.context.support.TestPropertySourceUtils


class OasStubTestExecutionListener: AbstractTestExecutionListener() {

    override fun beforeTestExecution(testContext: TestContext) {
        if (isTarget(testContext)) {
            getTestService(testContext).ifPresent { service ->
                service.setup()
            }
        }
    }

    override fun afterTestExecution(testContext: TestContext) {
        if (isTarget(testContext)) {
            getTestService(testContext).ifPresent { service ->
                service.clear()
            }
        }
    }

    private fun getTestService(testContext: TestContext): Optional<OasStubTestService> {
        return try {
            val context = testContext.applicationContext
            if (context.containsBean(OAS_STUB_TEST_SERVICE_BEAN_NAME)) {
                Optional.of(context.getBean(OAS_STUB_TEST_SERVICE_BEAN_NAME, OasStubTestService::class.java))
            } else {
                Optional.empty()
            }
        } catch (e: Exception) {
            Optional.empty()
        }
    }

    private fun isTarget(testContext: TestContext) = !annotationMissing(testContext)
    private fun annotationMissing(testContext: TestContext) = !TestContextAnnotationUtils.hasAnnotation(testContext.testClass, AutoConfigureOasStub::class.java)
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
                val randomPort = randomTcpPort()
                val source = (propertySource[TestPropertySourceUtils.INLINED_PROPERTIES_PROPERTY_SOURCE_NAME] as MapPropertySource).source
                source[SERVER_PORT] = randomPort
            }
            if (!propertySource.contains(OasStubTestProperties.OAS_STUB_TEST_PROPERTY_PREFIX) && isPortShared(environment)) {
                val oasServerSource = mutableMapOf<String, Any>()
                oasServerSource["${OasStubTestProperties.OAS_STUB_TEST_PROPERTY_PREFIX}.server.port"] = environment.getProperty(
                    SERVER_PORT
                )!!
                propertySource.addFirst(
                    MapPropertySource(
                        OasStubTestProperties.OAS_STUB_TEST_PROPERTY_PREFIX,
                        oasServerSource
                    ))
            }
        }
    }

}