package io.github.ktakashi.oas.model

enum class PluginType {
    GROOVY
}
data class PluginDefinition(val applicationName: String,
                            val apiPath: String,
                            val type: PluginType,
                            val script: String)

data class ApiOptions(val shouldValidate: Boolean = true)
data class ApiDefinition(val applicationName: String,
                         val apiOptions: Map<String, ApiOptions>,
                         val staticRequestHeaders: Map<String, List<String>>,
                         val staticResponseHeaders: Map<String, List<String>>,
                         val api: String, // Raw Swagger or OAS definition
                         val plugins: Map<String, PluginDefinition>,
                         val apiData: Map<String, ByteArray>)
