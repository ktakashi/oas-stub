package io.github.ktakashi.oas.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.Duration
import java.util.SortedMap
import java.util.TreeMap
import kotlin.time.DurationUnit
import kotlin.time.toTimeUnit

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

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes(
        JsonSubTypes.Type(value = ApiFailureNone::class, name = "none"),
        JsonSubTypes.Type(value = ApiProtocolFailure::class, name = "protocol"),
        JsonSubTypes.Type(value = ApiHttpError::class, name = "http")
)
sealed interface ApiFailure
data object ApiFailureNone: ApiFailure
data object ApiProtocolFailure: ApiFailure
data class ApiHttpError(val status: Int = 500): ApiFailure
data class ApiLatency(val interval: Long, val unit: DurationUnit = DurationUnit.SECONDS) {
    fun toDuration(): Duration = Duration.of(interval, unit.toTimeUnit().toChronoUnit())
}

data class ApiOptions
@JvmOverloads constructor(val shouldValidate: Boolean? = null,
                          val latency: ApiLatency? = null,
                          val failure: ApiFailure? = null,
                          val shouldMonitor: Boolean? = null): MergeableApiConfig<ApiOptions> {
    override fun merge(other: ApiOptions) = ApiOptions(shouldValidate = shouldValidate ?: other.shouldValidate,
        latency = latency ?: other.latency,
        failure = failure ?: other.failure,
        shouldMonitor = shouldMonitor ?: other.shouldMonitor)
}

data class ApiHeaders
@JvmOverloads constructor(val request: SortedMap<String, List<String>> = sortedMapOf(String.CASE_INSENSITIVE_ORDER),
                          val response: SortedMap<String, List<String>> = sortedMapOf(String.CASE_INSENSITIVE_ORDER)): MergeableApiConfig<ApiHeaders> {
    override fun merge(other: ApiHeaders): ApiHeaders = ApiHeaders(
            TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER).apply {
                putAll(other.request)
                putAll(request)
            },
            TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER).apply {
                putAll(other.response)
                putAll(response)
            })

}

class ApiData
@JvmOverloads constructor(private val delegate: Map<String, Any> = mapOf()): Map<String, Any> by delegate, MergeableApiConfig<ApiData> {
    override fun merge(other: ApiData): ApiData = ApiData(other + this)

    fun asMap(): Map<String, Any> = this
}

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type"
)
@JsonSubTypes(
        JsonSubTypes.Type(value = ApiFixedDelay::class, name = "fixed")
)
sealed interface ApiDelay: MergeableApiConfig<ApiDelay> {
    companion object {
        @JvmStatic
        val DEFAULT_DURATION_UNIT = DurationUnit.MILLISECONDS
    }
    val delayUnit: DurationUnit?
    override fun merge(other: ApiDelay): ApiDelay = this
}

@JsonTypeName("fixed")
data class ApiFixedDelay(override val delayUnit: DurationUnit? = ApiDelay.DEFAULT_DURATION_UNIT,
                         val delay: Long): ApiDelay

interface ApiCommonConfigurations<T: ApiCommonConfigurations<T>> {
    val headers: ApiHeaders?
    val options: ApiOptions?
    val data: ApiData?
    val delay: ApiDelay?
    fun updateHeaders(headers: ApiHeaders?): T
    fun updateOptions(options: ApiOptions?): T
    fun updateData(data: ApiData?): T
    fun updateDelay(delay: ApiDelay?): T
}

data class ApiConfiguration
@JvmOverloads constructor(override val headers: ApiHeaders? = null,
                          override val options: ApiOptions? = null,
                          override val data: ApiData? = null,
                          override val delay: ApiDelay? = null,
                          val plugin: PluginDefinition? = null): ApiCommonConfigurations<ApiConfiguration> {
    fun updatePlugin(plugin: PluginDefinition?) = ApiConfiguration(headers, options, data, delay, plugin)
    override fun updateHeaders(headers: ApiHeaders?): ApiConfiguration = ApiConfiguration(headers, options, data, delay, plugin)
    override fun updateOptions(options: ApiOptions?): ApiConfiguration = ApiConfiguration(headers, options, data, delay, plugin)
    override fun updateData(data: ApiData?): ApiConfiguration = ApiConfiguration(headers, options, data, delay, plugin)
    override fun updateDelay(delay: ApiDelay?): ApiConfiguration = ApiConfiguration(headers, options, data, delay, plugin)
}

data class ApiDefinitions
@JvmOverloads constructor(val specification: String? = null,
                          val configurations: Map<String, ApiConfiguration>? = null,
                          // global data, these are applied to all APIs of this context
                          override val headers: ApiHeaders? = null,
                          override val options: ApiOptions? = null,
                          override val data: ApiData? = null,
                          override val delay: ApiDelay? = null): ApiCommonConfigurations<ApiDefinitions> {

    fun updateSpecification(specification: String) =
            ApiDefinitions(specification, configurations, headers, options, data, delay)
    fun updateConfiguration(path: String, configuration: ApiConfiguration) =
            ApiDefinitions(specification, mapOf(path to configuration).let { configurations?.plus(it) ?: it } , headers, options, data, delay)

    fun updateConfigurations(configurations: Map<String, ApiConfiguration>?) = ApiDefinitions(specification, configurations, headers, options, data, delay)

    override fun updateHeaders(headers: ApiHeaders?) = ApiDefinitions(specification, configurations, headers, options, data, delay)
    override fun updateOptions(options: ApiOptions?) = ApiDefinitions(specification, configurations, headers, options, data, delay)
    override fun updateData(data: ApiData?) = ApiDefinitions(specification, configurations, headers, options, data, delay)
    override fun updateDelay(delay: ApiDelay?) = ApiDefinitions(specification, configurations, headers, options, data, delay)
}
