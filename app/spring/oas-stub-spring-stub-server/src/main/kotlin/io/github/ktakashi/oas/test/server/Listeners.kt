package io.github.ktakashi.oas.test.server

import io.github.ktakashi.oas.test.OAS_STUB_SERVER_BEAN_NAME
import io.github.ktakashi.oas.test.findAvailableTcpPort
import org.springframework.boot.context.event.ApplicationPreparedEvent
import org.springframework.context.ApplicationListener
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.test.context.TestContext
import org.springframework.test.context.TestContextAnnotationUtils
import org.springframework.test.context.support.AbstractTestExecutionListener

class OasStubServerApplicationListener: ApplicationListener<ApplicationPreparedEvent> {
    override fun onApplicationEvent(event: ApplicationPreparedEvent) {
        registerPorts(event.applicationContext.environment)
    }

    private fun registerPorts(environment: ConfigurableEnvironment) {
        val httpPortProp = "${OAS_STUB_SERVER_PROPERTY_PREFIX}.port"
        environment.getProperty(httpPortProp, Int::class.java)?.let {
            if (it == 0) {
                registerDynamicPort(environment, httpPortProp, 20000..22500)
            } else {
                registerPort(environment, httpPortProp, it)
            }
        } ?: registerDynamicPort(environment, httpPortProp, 20000..22500)

        val httpsPortProp = "${OAS_STUB_SERVER_PROPERTY_PREFIX}.https-port"
        environment.getProperty(httpsPortProp, Int::class.java)?.let {
            when {
                it == 0 -> registerDynamicPort(environment, httpsPortProp, 22500..25000)
                it < 0 -> Unit
                else -> registerPort(environment, httpsPortProp, it)
            }
        }
    }

    private fun registerDynamicPort(environment: ConfigurableEnvironment, property: String, range: IntRange) {
        val port = findAvailableTcpPort(range.first, range.last)
        registerPort(environment, property, port)
    }

    private fun registerPort(environment: ConfigurableEnvironment, property: String, port: Int) {
        val mapSource = getOasStubServerSource(environment)
        mapSource[property] = port
    }

    @Suppress("UNCHECKED_CAST")
    private fun getOasStubServerSource(environment: ConfigurableEnvironment): MutableMap<String, Any> {
        val propertySources = environment.propertySources
        if (propertySources.contains(OAS_STUB_SERVER_PROPERTY_PREFIX)) {
            propertySources.remove(OAS_STUB_SERVER_BEAN_NAME)?.let {
                propertySources.addFirst(it)
            }
        } else {
            propertySources.addFirst(MapPropertySource(OAS_STUB_SERVER_PROPERTY_PREFIX, mutableMapOf()))
        }
        return propertySources[OAS_STUB_SERVER_PROPERTY_PREFIX]!!.source as MutableMap<String, Any>
    }

}

class OasStubServerTestExecutionListener: AbstractTestExecutionListener() {
    override fun prepareTestInstance(testContext: TestContext) {
        if (isTarget(testContext)) {
            val config = oasStubServerConfiguration(testContext)
            if (!config.isRunning) {
                config.init()
            }
            config.start()
        }
    }

    override fun afterTestClass(testContext: TestContext) {
        if (isTarget(testContext)) {
            val config = oasStubServerConfiguration(testContext)
            config.stop()
        }
    }

    override fun afterTestMethod(testContext: TestContext) {
        if (isTarget(testContext)) {
            val config = oasStubServerConfiguration(testContext)
            if (config.serverProperties.resetConfigurationAfterEachTest) {
                config.resetConfiguration()
            }
        }
    }

    private fun oasStubServerConfiguration(testContext: TestContext) = testContext.applicationContext.getBean(OasStubServerConfiguration::class.java)

    private fun isTarget(testContext: TestContext) =
        !(applicationContextBroken(testContext) || annotationMissing(testContext) || configurationMissing(testContext))

    private fun configurationMissing(testContext: TestContext) = !testContext.applicationContext.containsBean(OAS_STUB_SERVER_CONFIGURATION_BEAN_NAME)

    private fun annotationMissing(testContext: TestContext) = !TestContextAnnotationUtils.hasAnnotation(testContext.testClass, AutoConfigureOasStubServer::class.java)

    private fun applicationContextBroken(testContext: TestContext) = try {
        testContext.applicationContext
        false
    } catch (e: Exception) {
        true
    }
}