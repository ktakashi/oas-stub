package io.github.ktakashi.oas.api.plugin

import io.github.ktakashi.oas.api.http.ResponseContext

/**
 * API Plugin
 *
 * Plugins must implement this interface
 */
fun interface ApiPlugin {
    fun customize(pluginContext: PluginContext): ResponseContext
}
