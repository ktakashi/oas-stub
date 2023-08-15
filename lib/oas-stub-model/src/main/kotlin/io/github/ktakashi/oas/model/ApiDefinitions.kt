package io.github.ktakashi.oas.model

enum class PluginType {
    GROOVY
}
data class PluginDefinition(val type: PluginType, val script: String)

fun interface MergeableApiConfig<T: MergeableApiConfig<T>> {
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
@JvmOverloads constructor(val request: Map<String, List<String>> = sortedMapOf(String.CASE_INSENSITIVE_ORDER),
                          val response: Map<String, List<String>> = sortedMapOf(String.CASE_INSENSITIVE_ORDER)): MergeableApiConfig<ApiHeaders> {
    override fun merge(other: ApiHeaders): ApiHeaders = ApiHeaders(other.request + request, other.response + response)
}

class ApiData
@JvmOverloads constructor(private val delegate: Map<String, Any> = mapOf()): Map<String, Any> by delegate, MergeableApiConfig<ApiData> {
    override fun merge(other: ApiData): ApiData = ApiData(other + this)
}

interface ApiCommonConfigurations<T: ApiCommonConfigurations<T>> {
    val headers: ApiHeaders?
    val options: ApiOptions?
    val data: ApiData?
    fun updateHeaders(headers: ApiHeaders?): T
    fun updateOptions(options: ApiOptions?): T
    fun updateData(data: ApiData?): T
}

data class ApiConfiguration
@JvmOverloads constructor(override val headers: ApiHeaders? = null,
                          override val options: ApiOptions? = null,
                          override val data: ApiData? = null,
                          val plugin: PluginDefinition? = null): ApiCommonConfigurations<ApiConfiguration> {
    fun updatePlugin(plugin: PluginDefinition?) = ApiConfiguration(headers, options, data, plugin)
    override fun updateHeaders(headers: ApiHeaders?): ApiConfiguration = ApiConfiguration(headers, options, data, plugin)
    override fun updateOptions(options: ApiOptions?): ApiConfiguration = ApiConfiguration(headers, options, data, plugin)
    override fun updateData(data: ApiData?): ApiConfiguration = ApiConfiguration(headers, options, data, plugin)
}

data class ApiDefinitions
@JvmOverloads constructor(val specification: String,
                          val configurations: Map<String, ApiConfiguration> = mapOf(),
                          // global data, these are applied to all APIs of this context
                          override val headers: ApiHeaders? = null,
                          override val options: ApiOptions? = null,
                          override val data: ApiData? = null): ApiCommonConfigurations<ApiDefinitions> {

    fun updateSpecification(specification: String) =
            ApiDefinitions(specification, configurations, headers, options, data)
    fun updateConfiguration(path: String, configuration: ApiConfiguration) =
            ApiDefinitions(specification, configurations + mapOf(path to configuration), headers, options, data)

    fun updateConfigurations(configurations: Map<String, ApiConfiguration>) = ApiDefinitions(specification, configurations, headers, options, data)

    override fun updateOptions(options: ApiOptions?) = ApiDefinitions(specification, configurations, headers, options, data)

    override fun updateHeaders(headers: ApiHeaders?) = ApiDefinitions(specification, configurations, headers, options, data)
    override fun updateData(data: ApiData?) = ApiDefinitions(specification, configurations, headers, options, data)
}
