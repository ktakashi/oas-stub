@file:JvmName("OasStubGuiceInjectors")
package io.github.ktakashi.oas.guice.injector

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.Stage
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceConfiguration
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceServerConfiguration
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceWebConfiguration
import io.github.ktakashi.oas.guice.modules.OasStubGuiceEngineModule
import io.github.ktakashi.oas.guice.modules.OasStubGuiceServerModule
import io.github.ktakashi.oas.guice.modules.OasStubGuiceWebModule

/**
 * Creates a Guice injector which contains OAS API stub engine components.
 *
 * This method creates with [Stage.DEVELOPMENT]
 *
 * @param configuration OasStub configuration
 * @param modules Extra modules to load into the returning injector
 */
fun createInjector(configuration: OasStubGuiceConfiguration, vararg modules: Module): Injector =
    createInjector(Stage.DEVELOPMENT, configuration, *modules)

/**
 * Creates a Guice injector which contains OAS API stub engine components.
 *
 * @param stage Injector stage
 * @param configuration OasStub configuration
 * @param modules Extra modules to load into the returning injector
 */
fun createInjector(stage: Stage, configuration: OasStubGuiceConfiguration, vararg modules: Module): Injector =
    Guice.createInjector(stage, OasStubGuiceEngineModule(configuration), *modules)


/**
 * Creates a Guice injector which contains OAS API stub web components.
 *
 * This method creates with [Stage.DEVELOPMENT]
 *
 * @param configuration OasStub web configuration
 * @param modules Extra modules to load into the returning injector* Creates a Guice injector which contains OAS API stub engine components.
 */
fun createWebInjector(configuration: OasStubGuiceWebConfiguration, vararg modules: Module): Injector =
    createWebInjector(Stage.DEVELOPMENT, configuration, *modules)

/**
 * Creates a Guice injector which contains OAS API stub web components.
 *
 * @param stage Injector stage
 * @param configuration OasStub web configuration
 * @param modules Extra modules to load into the returning injector* Creates a Guice injector which contains OAS API stub engine components.
 */
fun createWebInjector(stage: Stage, configuration: OasStubGuiceWebConfiguration, vararg modules: Module): Injector =
    Guice.createInjector(stage, OasStubGuiceWebModule(configuration), *modules)

/**
 * Creates a Guice injector which contains OAS API stub server components.
 *
 * This method creates with [Stage.DEVELOPMENT]
 *
 * @param configuration OasStub server configuration
 * @param modules Extra modules to load into the returning injector* Creates a Guice injector which contains OAS API stub engine components.
 */
fun createServerInjector(configuration: OasStubGuiceServerConfiguration, vararg modules: Module): Injector =
    createServerInjector(Stage.DEVELOPMENT, configuration, *modules)

/**
 * Creates a Guice injector which contains OAS API stub web components.
 *
 * @param stage Injector stage
 * @param configuration OasStub server configuration
 * @param modules Extra modules to load into the returning injector* Creates a Guice injector which contains OAS API stub engine components.
 */
fun createServerInjector(stage: Stage, configuration: OasStubGuiceServerConfiguration, vararg modules: Module): Injector =
    Guice.createInjector(stage, OasStubGuiceServerModule(configuration), *modules)