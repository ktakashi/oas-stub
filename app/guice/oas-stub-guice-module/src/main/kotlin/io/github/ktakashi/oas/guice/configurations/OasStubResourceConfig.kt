package io.github.ktakashi.oas.guice.configurations

import com.google.inject.Injector
import io.github.ktakashi.oas.jersey.OAS_APPLICATION_PATH_CONFIG
import io.github.ktakashi.oas.jersey.OasStubResourceConfig
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.servlet.ServletContext
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.jersey.server.ResourceConfig
import org.jvnet.hk2.guice.bridge.api.GuiceBridge
import org.jvnet.hk2.guice.bridge.api.GuiceIntoHK2Bridge

fun interface ResourceConfigCustomizer {
    fun customize(resourceConfig: ResourceConfig)
}

class OasStubGuiceResourceConfig
@Inject constructor(@Named(OAS_APPLICATION_PATH_CONFIG) adminPrefix: String,
                    serviceLocator: ServiceLocator,
                    servletContext: ServletContext)
    : OasStubResourceConfig(adminPrefix) {
    init {
        val injector = servletContext.getAttribute(Injector::class.java.name) as Injector
        GuiceBridge.getGuiceBridge().initializeGuiceBridge(serviceLocator)
        val guiceBridge = serviceLocator.getService(GuiceIntoHK2Bridge::class.java)
        guiceBridge.bridgeGuiceInjector(injector)

        val config = injector.getInstance(OasStubGuiceConfiguration::class.java)
        config.resourceConfigCustomizers.forEach { customizer -> customizer.customize(this) }
    }
}