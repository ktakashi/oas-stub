package io.github.ktakashi.oas.model

enum class PluginType {
    GROOVY
}
data class PluginDefinition(val type: PluginType, val script: String)

data class ApiOptions
@JvmOverloads constructor(val shouldValidate: Boolean? = null) {
    fun merge(other: ApiOptions) = ApiOptions(shouldValidate = shouldValidate ?: other.shouldValidate)
}

data class ApiHeaders
@JvmOverloads constructor(val request: Map<String, List<String>> = mapOf(),
                          val response: Map<String, List<String>> = mapOf())

data class ApiConfiguration
@JvmOverloads constructor(val headers: ApiHeaders = ApiHeaders(),
                          val options: ApiOptions = ApiOptions(),
                          val plugin: PluginDefinition? = null,
                          val data: Map<String, Any> = mapOf()) {
    fun updatePlugin(plugin: PluginDefinition?) = ApiConfiguration(headers, options, plugin, data)
    fun updateHeaders(headers: ApiHeaders): ApiConfiguration = ApiConfiguration(headers, options, plugin, data)
}

data class ApiDefinitions
@JvmOverloads constructor(val specification: String,
                          val configurations: Map<String, ApiConfiguration> = mapOf(),
                          // global data, these are applied to all APIs of this context
                          val headers: ApiHeaders = ApiHeaders(),
                          val options: ApiOptions = ApiOptions(),
                          val data: Map<String, Any> = mapOf()) {

    fun updateSpecification(specification: String) =
            ApiDefinitions(specification, configurations, headers, options, data)
    fun updateApiConfiguration(path: String, configuration: ApiConfiguration) =
            ApiDefinitions(specification, configurations + mapOf(path to configuration), headers, options, data)

    fun updateApiConfigurations(configurations: Map<String, ApiConfiguration>) = ApiDefinitions(specification, configurations, headers, options, data)

    fun updateApiOptions(options: ApiOptions) = ApiDefinitions(specification, configurations, headers, options, data)

    fun updateApiHeaders(headers: ApiHeaders) = ApiDefinitions(specification, configurations, headers, options, data)
    fun updateApiData(data: Map<String, Any>) = ApiDefinitions(specification, configurations, headers, options, data)
}
