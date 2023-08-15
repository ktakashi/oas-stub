package io.github.ktakashi.oas.model

enum class PluginType {
    GROOVY
}
data class PluginDefinition(val type: PluginType, val script: String)

interface MergeableApiConfig<T: MergeableApiConfig<T>> {
    /**
     * Merge the [other]. The [other]'s value will be overridden if there's the same value
     */
    fun merge(other: T): T
}

data class ApiOptions
@JvmOverloads constructor(val shouldValidate: Boolean? = null): MergeableApiConfig<ApiOptions> {
    override fun merge(other: ApiOptions) = ApiOptions(shouldValidate = shouldValidate ?: other.shouldValidate)
}

data class ApiHeaders
@JvmOverloads constructor(val request: Map<String, List<String>> = mapOf(),
                          val response: Map<String, List<String>> = mapOf()): MergeableApiConfig<ApiHeaders> {
    override fun merge(other: ApiHeaders): ApiHeaders = ApiHeaders(other.request + request, other.response + response)
}

class ApiData(private val delegate: Map<String, Any> = mapOf()): Map<String, Any> by delegate, MergeableApiConfig<ApiData> {
    override fun merge(other: ApiData): ApiData = ApiData(other + this)
}

interface ApiCommonConfigurations {
    val headers: ApiHeaders
    val options: ApiOptions
    val data: ApiData
}

data class ApiConfiguration
@JvmOverloads constructor(override val headers: ApiHeaders = ApiHeaders(),
                          override val options: ApiOptions = ApiOptions(),
                          override val data: ApiData = ApiData(),
                          val plugin: PluginDefinition? = null): ApiCommonConfigurations {
    fun updatePlugin(plugin: PluginDefinition?) = ApiConfiguration(headers, options, data, plugin)
    fun updateHeaders(headers: ApiHeaders): ApiConfiguration = ApiConfiguration(headers, options, data, plugin)
}

data class ApiDefinitions
@JvmOverloads constructor(val specification: String,
                          val configurations: Map<String, ApiConfiguration> = mapOf(),
                          // global data, these are applied to all APIs of this context
                          override val headers: ApiHeaders = ApiHeaders(),
                          override val options: ApiOptions = ApiOptions(),
                          override val data: ApiData = ApiData()): ApiCommonConfigurations {

    fun updateSpecification(specification: String) =
            ApiDefinitions(specification, configurations, headers, options, data)
    fun updateApiConfiguration(path: String, configuration: ApiConfiguration) =
            ApiDefinitions(specification, configurations + mapOf(path to configuration), headers, options, data)

    fun updateApiConfigurations(configurations: Map<String, ApiConfiguration>) = ApiDefinitions(specification, configurations, headers, options, data)

    fun updateApiOptions(options: ApiOptions) = ApiDefinitions(specification, configurations, headers, options, data)

    fun updateApiHeaders(headers: ApiHeaders) = ApiDefinitions(specification, configurations, headers, options, data)
    fun updateApiData(data: ApiData) = ApiDefinitions(specification, configurations, headers, options, data)
}
