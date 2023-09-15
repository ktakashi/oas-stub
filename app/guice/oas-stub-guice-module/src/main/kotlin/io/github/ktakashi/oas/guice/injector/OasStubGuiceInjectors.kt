@file:JvmName("OasStubGuiceInjectors")
package io.github.ktakashi.oas.guice.injector

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import com.google.inject.Stage
import io.github.ktakashi.oas.guice.configurations.OasStubGuiceConfiguration
import io.github.ktakashi.oas.guice.modules.OasStubGuiceEngineModule

fun createGuiceInjector(configuration: OasStubGuiceConfiguration, vararg modules: Module): Injector =
    createGuiceInjector(Stage.DEVELOPMENT, configuration, *modules)


fun createGuiceInjector(stage: Stage, configuration: OasStubGuiceConfiguration, vararg modules: Module): Injector =
    Guice.createInjector(stage, OasStubGuiceEngineModule(configuration.servletPrefix),
        configuration.sessionStorageModule,
        configuration.persistentStorageModule,
        *modules)
