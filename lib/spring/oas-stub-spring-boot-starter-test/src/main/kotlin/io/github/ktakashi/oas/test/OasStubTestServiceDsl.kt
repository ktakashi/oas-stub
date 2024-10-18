package io.github.ktakashi.oas.test

import io.github.ktakashi.oas.model.ApiConfiguration
import io.github.ktakashi.oas.model.ApiConnectionError
import io.github.ktakashi.oas.model.ApiData
import io.github.ktakashi.oas.model.ApiDelay
import io.github.ktakashi.oas.model.ApiFailure
import io.github.ktakashi.oas.model.ApiFailureNone
import io.github.ktakashi.oas.model.ApiFixedDelay
import io.github.ktakashi.oas.model.ApiHeaders
import io.github.ktakashi.oas.model.ApiHttpError
import io.github.ktakashi.oas.model.ApiLatency
import io.github.ktakashi.oas.model.ApiOptions
import io.github.ktakashi.oas.model.ApiProtocolFailure
import io.github.ktakashi.oas.model.PluginDefinition
import io.github.ktakashi.oas.model.PluginType
import java.nio.charset.StandardCharsets
import java.util.SortedMap
import java.util.TreeMap
import kotlin.time.DurationUnit
import org.springframework.core.io.Resource

/**
 * Allow to update easily stub configuration
 *
 * Example:
 * ```kotlin
 * oasStubTestService("petstore") {
 *     headers {
 *         request {
 *             header("Extra-Request-Header", "value0", "value1")
 *         }
 *         response {
 *             header("Extra-Response-Header", "value0", "value1")
 *         }
 *     }
 *     configuration("/v1/pets/{id}") {
 *         data {
 *             // This form require default plugin
 *             entry("/v1/pets/2", 200) {
 *                 header("boo", "bar", "buz")
 *                 body("""{"id": 2,"name": "Pochi","tag": "dog"}""")
 *             }
 *         }
 *     }
 *     configuration("/v1/pets") {
 *         plugin(ClasspathResource("classpath:/plugins/PostPetPlugin.groovy"))
 *         data {
  *             entry("/v1/pets/2", mapOf("key" to "value"))
 *         }
 *     }
 * }
 * ```
 *
 */
fun OasStubTestService.context(context: String, init: OasStubTestServiceDsl.() -> Unit) = OasStubTestServiceDsl(this.getTestApiContext(context), init).save()

class OasStubTestServiceDsl
    internal constructor(private var context: OasStubTestApiContext, private val init: OasStubTestServiceDsl.() -> Unit) {
    /**
     * Configure API specification
     */
    fun specification(specification: Resource) {
        context = context.updateSpecification(specification)
    }
    /**
     * Configure context level headers
     */
    fun headers(init: OasStubApiHeadersDsl.() -> Unit) {
        context = context.updateHeaders(OasStubApiHeadersDsl(init).save())
    }

    /**
     * Configure context level options
     */
    fun options(init: OasStubApiOptionsDsl.() -> Unit) {
        context = context.updateOptions(OasStubApiOptionsDsl(init).save())
    }

    /**
     * Configure context level delay
     */
    fun delay(init: OasStubApiDelayDsl.() -> Unit) {
        context = context.updateDelay(OasStubApiDelayDsl(init).save())
    }
    /**
     * Configure API level configuration
     */
    fun configuration(path: String, init: OasStubApiConfigurationDsl.() -> Unit) {
        context = context.updateApi(path, OasStubApiConfigurationDsl(init).save())
    }
    internal fun save() {
        init()
        return context.save()
    }
}

class OasStubApiConfigurationDsl internal  constructor(private val init: OasStubApiConfigurationDsl.() -> Unit) {
    companion object {
        private val defaultPluginDefinition = OasStubTestPlugin().toPluginDefinition()
    }
    private var apiConfiguration =  ApiConfiguration(plugin = defaultPluginDefinition)

    /**
     * Configure API level headers
     */
    fun headers(init: OasStubApiHeadersDsl.() -> Unit) {
        apiConfiguration = apiConfiguration.updateHeaders(OasStubApiHeadersDsl(init).save())
    }

    /**
     * Configure API level data
     */
    fun data(init: OasStubApiDataDsl.() -> Unit) {
        apiConfiguration = apiConfiguration.updateData(OasStubApiDataDsl(init).save())
    }

    /**
     * Configure API level delay
     */
    fun delay(init: OasStubApiDelayDsl.() -> Unit) {
        apiConfiguration = apiConfiguration.updateDelay(OasStubApiDelayDsl(init).save())
    }

    /**
     * Configure default plugin.
     *
     * By default, default plugin is configured, so only for documentation purpose
     */
    fun defaultPlugin() {
        apiConfiguration = apiConfiguration.updatePlugin(defaultPluginDefinition)
    }

    /**
     * Configure plugin
     *
     * [script] is a raw plugin script
     */
    fun plugin(script: String, type: PluginType = PluginType.GROOVY) {
        val plugin = PluginDefinition(type, script)
        apiConfiguration = apiConfiguration.updatePlugin(plugin)
    }

    /**
     * Configure plugin
     *
     * [resource] can be any Spring resource, e.g. [org.springframework.core.io.ClassPathResource]
     */
    fun plugin(resource: Resource, type: PluginType = PluginType.GROOVY) {
        val plugin = PluginDefinition(type, resource.getContentAsString(StandardCharsets.UTF_8))
        apiConfiguration = apiConfiguration.updatePlugin(plugin)
    }

    /**
     * Configure API level options
     */
    fun options(init: OasStubApiOptionsDsl.() -> Unit) {
        apiConfiguration = apiConfiguration.updateOptions(OasStubApiOptionsDsl(init).save())
    }

    internal fun save(): ApiConfiguration {
        init()
        return apiConfiguration
    }
}

class OasStubApiDataDsl internal  constructor(private val init: OasStubApiDataDsl.() -> Unit) {
    private var apiData = mutableMapOf<String, Any>()

    /**
     * Configure specialized API data
     *
     * This form require default plugin to have it work as you expect.
     */
    fun entry(key: String, status: Int, init: OasStubApiResponseDsl.() -> Unit) {
        apiData[key] = OasStubApiResponseDsl(status, init).save()
    }

    /**
     * Configure specialized API data
     *
     * [data] can be any type and can refer in plugin script
     */
    fun entry(key: String, data: Any) {
        apiData[key] = data
    }
    internal fun save(): ApiData {
        init()
        return ApiData(apiData)
    }
}

class OasStubApiResponseDsl internal  constructor(private val status: Int,  val init: OasStubApiResponseDsl.() -> Unit) {
    private var headers: MutableMap<String, List<String>> = TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER)
    private var body: String? = null

    fun header(name: String, vararg values: String) {
        headers[name] = values.toList()
    }
    fun body(body: String) {
        this.body = body
    }
    fun body(body: Resource) {
        this.body = body.getContentAsString(StandardCharsets.UTF_8)
    }

    internal fun save(): OasStubTestResources.DefaultResponseModel {
        init()
        return OasStubTestResources.DefaultResponseModel(status, headers, body)
    }
}

class OasStubApiDelayDsl internal constructor(private val init: OasStubApiDelayDsl.() -> Unit) {
    private var delay: ApiDelay? = null

    fun noDelay() {
        delay = null
    }

    fun fixed(delay: Long, delayUnit: DurationUnit = ApiDelay.DEFAULT_DURATION_UNIT) {
        this.delay = ApiFixedDelay(delay, delayUnit)
    }

    internal fun save(): ApiDelay? {
        init()
        return delay
    }
}

class OasStubApiOptionsDsl internal constructor(private val init: OasStubApiOptionsDsl.() -> Unit) {
    var shouldValidate: Boolean? = null
    var shouldMonitor: Boolean? = null
    var shouldRecord: Boolean? = null
    var latency: ApiLatency? = null
    var failure: ApiFailure? = null
    fun shouldValidate(shouldValidate: Boolean?) {
        this.shouldValidate = shouldValidate
    }
    fun shouldMonitor(shouldMonitor: Boolean?) {
        this.shouldMonitor = shouldMonitor
    }
    fun shouldRecord(shouldRecord: Boolean?) {
        this.shouldRecord = shouldRecord
    }
    fun latency(init: OasStubApiLatency.() -> Unit) {
        latency = OasStubApiLatency(init).save()
    }
    fun failure(init: OasStubApiFailureDsl.() -> Unit) {
        failure = OasStubApiFailureDsl(init).save()
    }
    internal fun save(): ApiOptions {
        init()
        return ApiOptions(shouldValidate = shouldValidate,
            shouldMonitor = shouldMonitor,
            shouldRecord = shouldRecord,
            latency = latency,
            failure = failure)
    }
}

class OasStubApiLatency internal constructor(private val init: OasStubApiLatency.() -> Unit) {
    var interval: Long? = null
    var unit: DurationUnit = DurationUnit.SECONDS

    fun interval(interval: Long?) {
        this.interval = interval
    }

    fun unit(unit: DurationUnit) {
        this.unit = unit
    }

    internal fun save(): ApiLatency? {
        init()
        return interval?.let {
            ApiLatency(it, unit)
        }
    }
}

class OasStubApiFailureDsl internal constructor(private val init: OasStubApiFailureDsl.() -> Unit) {
    private var failure: ApiFailure = ApiFailureNone
    fun none() {
        failure = ApiFailureNone
    }
    fun protocol() {
        failure = ApiProtocolFailure
    }
    fun status(status: Int) {
        failure = ApiHttpError(status)
    }
    fun connection() {
        failure = ApiConnectionError
    }
    internal fun save(): ApiFailure {
        init()
        return failure
    }
}

class OasStubApiHeadersDsl internal constructor(private val init: OasStubApiHeadersDsl.() -> Unit) {
    private var apiHeaders = ApiHeaders()
    fun request(init: OasStubApiHeaderDsl.() -> Unit) {
        apiHeaders = apiHeaders.copy(request = OasStubApiHeaderDsl(init).save())
    }
    fun response(init: OasStubApiHeaderDsl.() -> Unit) {
        apiHeaders = apiHeaders.copy(response = OasStubApiHeaderDsl(init).save())
    }
    internal fun save(): ApiHeaders {
        init()
        return apiHeaders
    }
}

class OasStubApiHeaderDsl internal  constructor(private val init: OasStubApiHeaderDsl.() -> Unit) {
    private val headers = TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER)

    fun header(name: String, vararg values: String) {
        headers[name] = values.toList()
    }

    infix fun String.to(value: List<String>) {
        headers[this] = value
    }

    internal fun save(): SortedMap<String, List<String>> {
        init()
        return headers
    }
}