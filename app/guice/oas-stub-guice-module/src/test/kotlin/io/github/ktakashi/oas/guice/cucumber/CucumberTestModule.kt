package io.github.ktakashi.oas.guice.cucumber

import com.google.inject.AbstractModule
import com.google.inject.Injector
import io.github.ktakashi.oas.guice.configurations.JettyWebAppContextCustomizer
import io.github.ktakashi.oas.guice.cucumber.rest.CustomController
import io.github.ktakashi.oas.guice.server.OasStubServer
import io.github.ktakashi.oas.test.cucumber.TestContext
import io.github.ktakashi.oas.test.cucumber.TestContextSupplier
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.eclipse.jetty.ee10.servlet.ServletHolder
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.servlet.ServletContainer

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

class CustomResourceConfig: ResourceConfig() {
    init {
        register(CustomController::class.java)
    }
}

internal val webAppContextCustomizer = JettyWebAppContextCustomizer { webAppContext ->
    val holder = ServletHolder(ServletContainer::class.java).apply {
        setInitParameter("jakarta.ws.rs.Application", CustomResourceConfig::class.java.name)
    }
    webAppContext.addServlet(holder, "/*")
}