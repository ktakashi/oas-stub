package io.github.ktakashi.oas.guice.cucumber.inmemory

import com.google.inject.Injector
import io.cucumber.guice.CucumberModules
import io.cucumber.guice.InjectorSource
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceServerConfiguration
import io.github.ktakashi.oas.guice.configurations.OasStubServerConnectorConfiguration
import io.github.ktakashi.oas.guice.cucumber.CucumberTestModule
import io.github.ktakashi.oas.guice.cucumber.webAppContextCustomizer
import io.github.ktakashi.oas.guice.injector.createServerInjector

class InMemoryInjectorSource: InjectorSource {
    override fun getInjector(): Injector = createServerInjector(
        OasStubGuiceServerConfiguration.builder()
            .jettyWebAppContextCustomizers(setOf(webAppContextCustomizer))
            .serverConnectors(listOf(OasStubServerConnectorConfiguration.builder("http").port(0).build()))
            .build(),
        CucumberTestModule(),
        CucumberModules.createScenarioModule())
}