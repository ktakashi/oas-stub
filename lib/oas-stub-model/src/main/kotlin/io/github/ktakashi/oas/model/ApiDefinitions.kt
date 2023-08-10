package io.github.ktakashi.oas.model

import java.util.Optional

enum class PluginType {
    GROOVY
}
data class PluginDefinition(val type: PluginType,
                            val script: String)

data class ApiOptions(val shouldValidate: Boolean = true) {
    fun merge(other: ApiOptions) = ApiOptions(shouldValidate = shouldValidate || other.shouldValidate)
}

data class Headers(val request: Map<String, List<String>> = mapOf(),
                   val response: Map<String, List<String>> = mapOf())

data class ApiConfiguration(val headers: Headers = Headers(),
                            val apiOptions: ApiOptions = ApiOptions(),
                            val plugin: Optional<PluginDefinition> = Optional.empty(),
                            val apiData: Map<String, Any> = mapOf()) {
    fun updatePlugin(pluginDefinition: PluginDefinition) =
            ApiConfiguration(headers, apiOptions, Optional.of(pluginDefinition), apiData)
}

data class ApiDefinitions(val applicationName: String,
                          val api: String, // Raw Swagger or OAS definition
                          val apiConfigurations: Map<String, ApiConfiguration> = mapOf(),
                          // global data, these are applied to all APIs of this context
                          val headers: Headers = Headers(),
                          val apiOptions: ApiOptions = ApiOptions(),
                          val apiData: Map<String, Any> = mapOf()) {
    fun updateApiConfiguration(path: String, apiConfiguration: ApiConfiguration) =
            ApiDefinitions(applicationName, api, apiConfigurations + mapOf(path to apiConfiguration),
                    headers, apiOptions, apiData)
}
