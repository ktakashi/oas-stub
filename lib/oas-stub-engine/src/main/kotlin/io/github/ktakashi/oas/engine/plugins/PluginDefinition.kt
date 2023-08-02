package io.github.ktakashi.oas.engine.plugins

enum class PluginType {
    GROOVY
}
data class PluginDefinition(val applicationName: String,
                            val apiPath: String,
                            val type: PluginType,
                            val script: String)
