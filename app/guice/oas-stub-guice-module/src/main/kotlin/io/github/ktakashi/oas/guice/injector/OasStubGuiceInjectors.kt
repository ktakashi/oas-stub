@file:JvmName("OasStubGuiceInjectors")
package io.github.ktakashi.oas.guice.injector

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.Stage
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceServerConfiguration
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceWebConfiguration
import io.github.ktakashi.oas.guice.modules.OasStubGuiceEngineModule
import io.github.ktakashi.oas.guice.modules.OasStubGuiceServerModule
import io.github.ktakashi.oas.guice.modules.OasStubGuiceWebModule

fun createInjector(configuration: OasStubGuiceWebConfiguration, vararg modules: Module): Injector =
    createInjector(Stage.DEVELOPMENT, configuration, *modules)


fun createInjector(stage: Stage, configuration: OasStubGuiceWebConfiguration, vararg modules: Module): Injector =
    Guice.createInjector(stage, OasStubGuiceEngineModule(configuration), *modules)


fun createWebInjector(configuration: OasStubGuiceWebConfiguration, vararg modules: Module): Injector =
    createWebInjector(Stage.DEVELOPMENT, configuration, *modules)

fun createWebInjector(stage: Stage, configuration: OasStubGuiceWebConfiguration, vararg modules: Module): Injector =
    Guice.createInjector(stage, OasStubGuiceWebModule(configuration), *modules)

fun createServerInjector(configuration: OasStubGuiceServerConfiguration, vararg modules: Module): Injector =
    createServerInjector(Stage.DEVELOPMENT, configuration, *modules)

fun createServerInjector(stage: Stage, configuration: OasStubGuiceServerConfiguration, vararg modules: Module): Injector =
    Guice.createInjector(stage, OasStubGuiceServerModule(configuration), *modules)