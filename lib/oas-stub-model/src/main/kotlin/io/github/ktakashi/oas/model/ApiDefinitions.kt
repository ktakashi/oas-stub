package io.github.ktakashi.oas.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import java.time.Duration
import java.util.SortedMap
import java.util.TreeMap
import java.util.concurrent.TimeUnit
import kotlin.time.DurationUnit
import kotlin.time.toDurationUnit
import kotlin.time.toTimeUnit

enum class PluginType {
    GROOVY
}

/**
 * Plugin definition
 */
data class PluginDefinition(
    /**
     * Plugin type.
     */
    val type: PluginType,
    /**
     * Plugin script
     */
    val script: String) {
    companion object {
        /**
         * Creates a [PluginDefinition] with [type] of [PluginType.GROOVY].
         *
         * [script] must be Groovy script
         */
        @JvmStatic
        fun groovyPlugin(script: String) = PluginDefinition(PluginType.GROOVY, script)
    }
}

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

/**
 * No failure
 */
data object ApiFailureNone: ApiFailure

/**
 * HTTP protocol error.
 *
 * At this moment, the API will return invalid HTTP status
 */
data object ApiProtocolFailure: ApiFailure

/**
 * HTTP failure.
 *
 * The API will return the [status] code, default `500`
 */
data class ApiHttpError(val status: Int = 500): ApiFailure

/**
 * API Latency
 *
 * This option emulates high latency network.
 *
 * If this options is set, then a byte of the response content will be emitted
 * every [interval].
 *
 * NOTE: Only content, not header part.
 */
data class ApiLatency
@JvmOverloads constructor(
    /**
     * The interval
     */
    val interval: Long,
    /**
     * Interval unit.
     *
     * Default second
     */
    val unit: DurationUnit = DurationUnit.SECONDS) {
    /**
     * Java friendly constructor
     */
    constructor(interval: Long, unit: TimeUnit): this(interval, unit.toDurationUnit())

    /**
     * Converts this to [Duration]
     */
    fun toDuration(): Duration = Duration.of(interval, unit.toTimeUnit().toChronoUnit())

    companion object {
        /**
         * Creates [ApiLatency] from given [duration]
         */
        @JvmStatic
        fun fromDuration(duration: Duration) = ApiLatency(duration.toNanos(), TimeUnit.NANOSECONDS)
    }
}

/**
 * API options
 */
data class ApiOptions
@JvmOverloads constructor(
    /**
     * Flag to control validation.
     *
     * The validation will be skipped iff the value is set to `false`
     */
    val shouldValidate: Boolean? = null,
    /**
     * API latency
     */
    val latency: ApiLatency? = null,
    /**
     * API failure option
     */
    val failure: ApiFailure? = null,
    /**
     * Flag to control monitoring / measurement.
     *
     * The monitoring will be skipped iff value is set to `false`
     */
    val shouldMonitor: Boolean? = null): MergeableApiConfig<ApiOptions> {
    private constructor(builder: Builder): this(builder.shouldValidate, builder.latency, builder.failure, builder.shouldMonitor)
    override fun merge(other: ApiOptions) = ApiOptions(shouldValidate = shouldValidate ?: other.shouldValidate,
        latency = latency ?: other.latency,
        failure = failure ?: other.failure,
        shouldMonitor = shouldMonitor ?: other.shouldMonitor)

    companion object {
        /**
         * Creates a builder
         */
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        var shouldValidate: Boolean? = null
            private set
        var latency: ApiLatency? = null
            private set
        var failure: ApiFailure? = null
            private set
        var shouldMonitor: Boolean? = null
            private set

        /**
         * Sets [shouldValidate]
         */
        fun shouldValidate(shouldValidate: Boolean?) = apply { this.shouldValidate = shouldValidate }

        /**
         * Sets [latency]
         */
        fun latency(latency: ApiLatency?) = apply { this.latency = latency }

        /**
         * Sets [failure]
         */
        fun failure(failure: ApiFailure?) = apply { this.failure = failure }

        /**
         * Sets [shouldMonitor]
         */
        fun shouldMonitor(shouldMonitor: Boolean?) = apply { this.shouldMonitor = shouldMonitor }

        /**
         * Builds [ApiOptions]
         */
        fun build() = ApiOptions(this)
    }
}

/**
 * API static headers
 */
data class ApiHeaders
@JvmOverloads constructor(
    /**
     * Request static headers.
     *
     * These headers will be populated before the validation.
     *
     * This is useful to emulate a situation when API requires mandatory headers which is injected by a proxy.
     */
    val request: SortedMap<String, List<String>> = sortedMapOf(String.CASE_INSENSITIVE_ORDER),
    /**
     * Response static headers.
     *
     * These headers will be populated before plugin
     */
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

/**
 * API data
 *
 * API data can be used in an API plugin.
 * This class can hold more or less anything as long as it's serializable on the storage.
 *
 * If the storage is in-memory, then this class can hold anything.
 */
class ApiData
/**
 * The constructor of the API data.
 *
 * [delegate] is the API data
 */
@JvmOverloads constructor(private val delegate: Map<String, Any> = mapOf()): Map<String, Any> by delegate, MergeableApiConfig<ApiData> {
    override fun merge(other: ApiData): ApiData = ApiData(other + this)

    /**
     * Return this API data as Map
     */
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
data class ApiFixedDelay
@JvmOverloads constructor(val delay: Long,
                          override val delayUnit: DurationUnit? = ApiDelay.DEFAULT_DURATION_UNIT): ApiDelay

interface ApiCommonConfigurations<T: ApiCommonConfigurations<T>> {
    val headers: ApiHeaders?
    val options: ApiOptions?
    val data: ApiData?
    val delay: ApiDelay?

    /**
     * Updates [headers] and return new instance of [T]
     */
    fun updateHeaders(headers: ApiHeaders?): T

    /**
     * Updates [options] and return new instance of [T]
     */
    fun updateOptions(options: ApiOptions?): T

    /**
     * Updates [data] and return new instance of [T]
     */
    fun updateData(data: ApiData?): T

    /**
     * Updates [delay] and return new instance of [T]
     */
    fun updateDelay(delay: ApiDelay?): T
}

/**
 * API configuration.
 *
 * This configuration is applied per APIs.
 * If an API has specific configuration, then this configuration overwrites the
 * context configuration.
 */
data class ApiConfiguration
@JvmOverloads constructor(
    /**
     * API specific headers
     */
    override val headers: ApiHeaders? = null,
    /**
     * API specific options
     */
    override val options: ApiOptions? = null,
    /**
     * API specific data
     */
    override val data: ApiData? = null,
    /**
     * API specific delay config
     */
    override val delay: ApiDelay? = null,
    /**
     * API Plugin
     */
    val plugin: PluginDefinition? = null): ApiCommonConfigurations<ApiConfiguration> {
    private constructor(builder: Builder): this(builder.headers, builder.options, builder.data, builder.delay, builder.plugin)

    /**
     * Updates [plugin] and returns new [ApiConfiguration] instance
     */
    fun updatePlugin(plugin: PluginDefinition?) = ApiConfiguration(headers, options, data, delay, plugin)
    override fun updateHeaders(headers: ApiHeaders?): ApiConfiguration = ApiConfiguration(headers, options, data, delay, plugin)
    override fun updateOptions(options: ApiOptions?): ApiConfiguration = ApiConfiguration(headers, options, data, delay, plugin)
    override fun updateData(data: ApiData?): ApiConfiguration = ApiConfiguration(headers, options, data, delay, plugin)
    override fun updateDelay(delay: ApiDelay?): ApiConfiguration = ApiConfiguration(headers, options, data, delay, plugin)

    companion object {
        /**
         * Creates a builder
         */
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        var headers: ApiHeaders? = null
            private set
        var options: ApiOptions? = null
            private set
        var data: ApiData? = null
            private set
        var delay: ApiDelay? = null
            private set
        var plugin: PluginDefinition? = null
            private set

        /**
         * Sets [headers]
         */
        fun headers(headers: ApiHeaders?) = apply { this.headers = headers }

        /**
         * Sets [options]
         */
        fun options(options: ApiOptions?) = apply { this.options = options }

        /**
         * Sets [data]
         */
        fun data(data: ApiData?) = apply { this.data = data }

        /**
         * Sets [delay]
         */
        fun delay(delay: ApiDelay?) = apply { this.delay = delay }

        /**
         * Sets [plugin]
         */
        fun plugin(plugin: PluginDefinition?) = apply { this.plugin = plugin }

        /**
         * Builds an [ApiConfiguration]
         */
        fun build() = ApiConfiguration(this)
    }
}

/**
 * API definition
 */
data class ApiDefinitions
@JvmOverloads constructor(
    /**
     * OAS or Swagger definition.
     *
     * The content can be either JSON or YAML
     */
    val specification: String? = null,
    /**
     * API configuration.
     *
     * The key must be a API path either URI template or concrete URI
     */
    val configurations: Map<String, ApiConfiguration>? = null,
    // global data, these are applied to all APIs of this context
    /**
     * Extra API headers.
     *
     * This header is applied on entire API context level.
     */
    override val headers: ApiHeaders? = null,
    /**
     * API options
     *
     * This options are applied on entire API context level.
     */
    override val options: ApiOptions? = null,
    /**
     * API data.
     *
     * This data can be retrieved all APIs defined in [specification]
     */
    override val data: ApiData? = null,
    /**
     * API delay.
     *
     * This delay is applied on entire API context level
     */
    override val delay: ApiDelay? = null): ApiCommonConfigurations<ApiDefinitions> {

    private constructor(builder: Builder): this(builder.specification, builder.configurations, builder.headers, builder.options, builder.data, builder.delay)

    /**
     * Replace with given [specification] and returns new [ApiDefinitions] instance
     */
    fun updateSpecification(specification: String) =
            ApiDefinitions(specification, configurations, headers, options, data, delay)
    /**
     * Associate given [path] and [configuration], then update [configurations] and returns new [ApiDefinitions] instance
     */
    fun updateConfiguration(path: String, configuration: ApiConfiguration) =
            ApiDefinitions(specification, mapOf(path to configuration).let { configurations?.plus(it) ?: it } , headers, options, data, delay)

    /**
     * Replace with given [configurations] and returns new [ApiDefinitions] instance
     */
    fun updateConfigurations(configurations: Map<String, ApiConfiguration>?) = ApiDefinitions(specification, configurations, headers, options, data, delay)

    override fun updateHeaders(headers: ApiHeaders?) = ApiDefinitions(specification, configurations, headers, options, data, delay)
    override fun updateOptions(options: ApiOptions?) = ApiDefinitions(specification, configurations, headers, options, data, delay)
    override fun updateData(data: ApiData?) = ApiDefinitions(specification, configurations, headers, options, data, delay)
    override fun updateDelay(delay: ApiDelay?) = ApiDefinitions(specification, configurations, headers, options, data, delay)

    companion object {
        /**
         * Returns a builder object
         */
        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        var specification: String? = null
            private set
        var configurations: Map<String, ApiConfiguration>? = null
            private set
        var headers: ApiHeaders? = null
            private set
        var options: ApiOptions? = null
            private set
        var data: ApiData? = null
            private set
        var delay: ApiDelay? = null
            private set

        /**
         * Sets [specification]
         */
        fun specification(specification: String?) = apply { this.specification = specification }

        /**
         * Sets [configurations]
         */
        fun configurations(configurations: Map<String, ApiConfiguration>?) = apply { this.configurations = configurations }

        /**
         * Associates [path] and [configuration] and updates [configurations]
         */
        fun configuration(path: String, configuration: ApiConfiguration) = apply {
            configurations = if (configurations == null) {
                mapOf(path to configuration)
            } else {
                configurations!! + (path to configuration)
            }
        }

        /**
         * Sets [headers]
         */
        fun headers(headers: ApiHeaders?) = apply { this.headers = headers }

        /**
         * Sets [options]
         */
        fun options(options: ApiOptions?) = apply { this.options = options }

        /**
         * Sets [data]
         */
        fun data(data: ApiData?) = apply { this.data = data }

        /**
         * Sets [delay]
         */
        fun delay(delay: ApiDelay?) = apply { this.delay = delay }

        /**
         * Builds an [ApiDefinitions]
         */
        fun build() = ApiDefinitions(this)
    }
}
