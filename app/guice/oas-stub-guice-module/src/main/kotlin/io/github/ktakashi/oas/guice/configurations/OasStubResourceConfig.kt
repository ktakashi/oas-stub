package io.github.ktakashi.oas.guice.configurations

import com.google.inject.Injector
import io.github.ktakashi.oas.guice.services.DelayableInterceptorBinder
import io.github.ktakashi.oas.jersey.OAS_APPLICATION_PATH_CONFIG
import io.github.ktakashi.oas.jersey.OasStubResourceConfig
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import jakarta.servlet.ServletContext
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.jersey.server.ResourceConfig
import org.jvnet.hk2.guice.bridge.api.GuiceBridge
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge

/**
 * OAS Stub admin resource customizer interface.
 *
 * This customizer can be used to customize admin endpoints
 * and/or add custom admin endpoints.
 */
fun interface ResourceConfigCustomizer {
    fun customize(resourceConfig: ResourceConfig)
}

/**
 * Guice bridge utility
 */
object OasStubGuiceBridgeUtil {
    /**
     * Setup Guice bridge.
     *
     * This method sets up default interceptors such as [Delayable] annotation interceptor.
     *
     * This example shows how to set up custom endpoint(s)
     *
     * ```kotlin
     * class CustomResourceConfig
     * @Inject constructor(serviceLocator: ServiceLocator, servletContext: ServletContext): ResourceConfig() {
     *     init {
     *         OasStubGuiceBridgeUtil.initializeGuiceBridge(this, serviceLocator, servletContext)
     *         // Register custom resources here
     *         register(CustomController::class.java)
     *     }
     * }
     * ```
     */
    @JvmStatic
    fun initializeGuiceBridge(resourceConfig: ResourceConfig, serviceLocator: ServiceLocator, servletContext: ServletContext): Injector {
        resourceConfig.register(DelayableInterceptorBinder())
        val injector = servletContext.getAttribute(Injector::class.java.name) as Injector
        GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator)
        val guiceBridge = serviceLocator.getService(GuiceIntoHK2Bridge::class.java)
        guiceBridge.bridgeGuiceInjector(injector)
        return injector
    }
}

@Named @Singleton
class OasStubGuiceResourceConfig
@Inject constructor(serviceLocator: ServiceLocator, servletContext: ServletContext)
    : OasStubResourceConfig("dummy") {
    init {
        val injector = OasStubGuiceBridgeUtil.initializeGuiceBridge(this, serviceLocator, servletContext)

        val config = injector.getInstance(OasStubGuiceWebConfiguration::class.java)
        config.resourceConfigCustomizers.forEach { customizer -> customizer.customize(this) }
        super.property(OAS_APPLICATION_PATH_CONFIG, config.oasStubConfiguration.adminPrefix)
    }
}