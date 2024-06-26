package io.github.ktakashi.oas.engine.plugins

import io.github.ktakashi.oas.api.plugin.ApiPlugin
import io.github.ktakashi.oas.model.PluginType

abstract class PluginCompiler(private val pluginType: PluginType) {
    abstract fun compileScript(script: String): Class<ApiPlugin>

    fun support(pluginType: PluginType): Boolean = this.pluginType == pluginType
}
