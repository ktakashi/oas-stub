package io.github.ktakashi.oas.plugin.apis

/**
 * API Plugin
 *
 * Plugins must implement this interface
 */
interface ApiPlugin {
    fun customize(pluginContext: PluginContext): ResponseContext
}
