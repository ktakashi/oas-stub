package io.github.ktakashi.oas.test

import io.github.ktakashi.oas.model.ApiConfiguration
import io.github.ktakashi.oas.model.ApiConnectionError
import io.github.ktakashi.oas.model.ApiData
import io.github.ktakashi.oas.model.ApiDelay
import io.github.ktakashi.oas.model.ApiEntryConfiguration
import io.github.ktakashi.oas.model.ApiFailure
import io.github.ktakashi.oas.model.ApiFailureNone
import io.github.ktakashi.oas.model.ApiFixedDelay
import io.github.ktakashi.oas.model.ApiHeaders
import io.github.ktakashi.oas.model.ApiHttpError
import io.github.ktakashi.oas.model.ApiLatency
import io.github.ktakashi.oas.model.ApiMethodConfiguration
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
 *             // or
 *             "X-Request-ID" to listOf("ID-1")
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
 *         // Per HTTP method configuration
 *         get {
 *             data {
 *                 entry("/v1/pets/3", 404) {
 *                     header("Request-ID", "1")
 *                 }
 *             }
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

open class OasStubApiEntryConfigurationDsl<D: OasStubApiEntryConfigurationDsl<D, T>, T: ApiEntryConfiguration<T>>(private val init: OasStubApiEntryConfigurationDsl<D, T>.() -> Unit, protected var apiEntryConfiguration: T) {
    companion object {
        @JvmStatic
        protected val defaultPluginDefinition = OasStubTestPlugin().toPluginDefinition()
    }
    /**
     * Configure API level headers
     */
    fun headers(init: OasStubApiHeadersDsl.() -> Unit) {
        apiEntryConfiguration = apiEntryConfiguration.updateHeaders(OasStubApiHeadersDsl(init).save())
    }

    /**
     * Configure API level data
     */
    fun data(init: OasStubApiDataDsl.() -> Unit) {
        apiEntryConfiguration = apiEntryConfiguration.updateData(OasStubApiDataDsl(init).save())
    }

    /**
     * Configure API level delay
     */
    fun delay(init: OasStubApiDelayDsl.() -> Unit) {
        apiEntryConfiguration = apiEntryConfiguration.updateDelay(OasStubApiDelayDsl(init).save())
    }

    /**
     * Configure default plugin.
     *
     * By default, default plugin is configured, so only for documentation purpose
     */
    fun defaultPlugin() {
        apiEntryConfiguration = apiEntryConfiguration.updatePlugin(defaultPluginDefinition)
    }

    /**
     * Configure plugin
     *
     * [script] is a raw plugin script
     */
    fun plugin(script: String, type: PluginType = PluginType.GROOVY) {
        val plugin = PluginDefinition(type, script)
        apiEntryConfiguration = apiEntryConfiguration.updatePlugin(plugin)
    }

    /**
     * Configure plugin
     *
     * [resource] can be any Spring resource, e.g. [org.springframework.core.io.ClassPathResource]
     */
    fun plugin(resource: Resource, type: PluginType = PluginType.GROOVY) {
        val plugin = PluginDefinition(type, resource.getContentAsString(StandardCharsets.UTF_8))
        apiEntryConfiguration = apiEntryConfiguration.updatePlugin(plugin)
    }

    /**
     * Configure API level options
     */
    fun options(init: OasStubApiOptionsDsl.() -> Unit) {
        apiEntryConfiguration = apiEntryConfiguration.updateOptions(OasStubApiOptionsDsl(init).save())
    }

    internal fun save(): T {
        init()
        return apiEntryConfiguration
    }
}

private typealias OasStubApiConfigurationDslFunc = OasStubApiEntryConfigurationDsl<OasStubApiConfigurationDsl, ApiConfiguration>.() -> Unit
class OasStubApiConfigurationDsl internal constructor(init: OasStubApiConfigurationDsl.() -> Unit)
    : OasStubApiEntryConfigurationDsl<OasStubApiConfigurationDsl, ApiConfiguration>(init as OasStubApiConfigurationDslFunc, ApiConfiguration(plugin = defaultPluginDefinition)) {
    /**
     * Specifying HTTP method and configure API per method configuration
     */
    fun method(method: String, init: OasStubApiMethodConfigurationDslFunc) {
        apiEntryConfiguration = apiEntryConfiguration.updateMethod(method, OasStubApiMethodConfigurationDsl(init).save())
    }

    /**
     * Configure API per method configuration of HEAD
     */
    fun head(init: OasStubApiMethodConfigurationDslFunc) = method("HEAD", init)
    /**
     * Configure API per method configuration of GET
     */
    fun get(init: OasStubApiMethodConfigurationDslFunc) = method("GET", init)
    /**
     * Configure API per method configuration of POST
     */
    fun post(init: OasStubApiMethodConfigurationDslFunc) = method("POST", init)
    /**
     * Configure API per method configuration of PUT
     */
    fun put(init: OasStubApiMethodConfigurationDslFunc) = method("PUT", init)
    /**
     * Configure API per method configuration of DELETE
     */
    fun delete(init: OasStubApiMethodConfigurationDslFunc) = method("DELETE", init)
    /**
     * Configure API per method configuration of PATCH
     */
    fun patch(init: OasStubApiMethodConfigurationDslFunc) = method("PATCH", init)
}

private typealias OasStubApiMethodConfigurationDslFunc = OasStubApiEntryConfigurationDsl<OasStubApiMethodConfigurationDsl, ApiMethodConfiguration>.() -> Unit
class OasStubApiMethodConfigurationDsl internal constructor(init: OasStubApiMethodConfigurationDslFunc)
    // Unlike ApiConfiguration, we don't specify plugin (it'll be propagated)
    : OasStubApiEntryConfigurationDsl<OasStubApiMethodConfigurationDsl, ApiMethodConfiguration>(init, ApiMethodConfiguration())

class OasStubApiDataDsl internal constructor(private val init: OasStubApiDataDsl.() -> Unit) {
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

    /**
     * Convenient method of [entry]
     */
    infix fun String.to(data: Any) {
        entry(this, data)
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
    private var latency: ApiLatency? = null
    private var failure: ApiFailure? = null
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
        header(name, values.toList())
    }

    fun header(name: String, values: List<String>) {
        headers[name] = values
    }

    infix fun String.to(value: List<String>) {
        header(this, value)
    }

    infix fun String.to(value: String) {
        header(this, listOf(value))
    }

    internal fun save(): SortedMap<String, List<String>> {
        init()
        return headers
    }
}