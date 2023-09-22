package io.github.ktakashi.oas.guice.cucumber

import com.google.inject.AbstractModule
import com.google.inject.Injector
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceBridgeUtil
import io.github.ktakashi.oas.guice.configurations.OasStubServerUtil
import io.github.ktakashi.oas.guice.cucumber.rest.CustomController
import io.github.ktakashi.oas.guice.server.OasStubServer
import io.github.ktakashi.oas.test.cucumber.TestContext
import io.github.ktakashi.oas.test.cucumber.TestContextSupplier
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.servlet.ServletContext
import org.glassfish.hk2.api.ServiceLocator
import org.glassfish.jersey.server.ResourceConfig

class CucumberTestModule: AbstractModule() {
    override fun configure() {
        bind(TestContextSupplier::class.java).to(GuiceTestContextSupplier::class.java)
    }
}

@Singleton
class GuiceTestContextSupplier
@Inject constructor(private val injector: Injector): TestContextSupplier {
    override fun get(): TestContext {
        val server = injector.getInstance(OasStubServer::class.java)
        return TestContext("http://localhost:${server.port()}", "/oas", "/__admin")
    }
}

class CustomResourceConfig
@Inject constructor(serviceLocator: ServiceLocator, servletContext: ServletContext): ResourceConfig() {
    init {
        OasStubGuiceBridgeUtil.initializeGuiceBridge(this, serviceLocator, servletContext)
        register(CustomController::class.java)
    }
}

internal val webAppContextCustomizer = OasStubServerUtil.resourceConfigServletCustomizer("/*", CustomResourceConfig::class.java)
