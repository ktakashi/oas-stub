package io.github.ktakashi.oas.engine.plugins

import io.github.ktakashi.oas.model.PluginType
import io.github.ktakashi.oas.plugin.apis.ApiPlugin

abstract class PluginCompiler(private val pluginType: PluginType) {
    abstract fun compileScript(script: String): Class<ApiPlugin>

    fun support(pluginType: PluginType): Boolean = this.pluginType == pluginType
}
