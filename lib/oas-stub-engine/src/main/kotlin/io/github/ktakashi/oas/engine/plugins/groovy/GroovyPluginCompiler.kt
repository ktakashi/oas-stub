package io.github.ktakashi.oas.engine.plugins.groovy

import groovy.lang.GroovyClassLoader
import io.github.ktakashi.oas.engine.plugins.PluginCompiler
import io.github.ktakashi.oas.model.PluginType
import io.github.ktakashi.oas.plugin.apis.ApiPlugin

class GroovyPluginCompiler: PluginCompiler(PluginType.GROOVY) {
    @Suppress("UNCHECKED_CAST")
    override fun compileScript(script: String): Class<ApiPlugin> {
        GroovyClassLoader().use {
            val clazz = it.parseClass(script)
            if (ApiPlugin::class.java.isAssignableFrom(clazz)) {
                return clazz as Class<ApiPlugin>
            }
            throw IllegalArgumentException("The script doesn't extend ApiPlugin: $clazz")
        }
    }
}
