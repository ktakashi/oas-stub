package io.github.ktakashi.oas.test

import com.fasterxml.jackson.core.JsonPointer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.ktakashi.oas.engine.apis.ApiRegistrationService
import io.github.ktakashi.oas.engine.apis.monitor.ApiObserver
import io.github.ktakashi.oas.engine.apis.record.ApiRecorder
import io.github.ktakashi.oas.model.ApiConfiguration
import io.github.ktakashi.oas.model.ApiData
import io.github.ktakashi.oas.model.ApiDefinitions
import io.github.ktakashi.oas.model.ApiDelay
import io.github.ktakashi.oas.model.ApiHeaders
import io.github.ktakashi.oas.model.ApiMetric
import io.github.ktakashi.oas.model.ApiOptions
import io.github.ktakashi.oas.model.ApiRecord
import io.github.ktakashi.oas.model.ApiRequestRecord
import io.github.ktakashi.oas.model.ApiResponseRecord
import java.util.Optional
import java.util.function.Predicate
import kotlin.jvm.optionals.getOrNull
import org.springframework.core.io.Resource
import org.springframework.web.util.UriTemplate

/**
 * Providing test utilities
 *
 * The service provides operations to API definition and API metrics.
 * Both of them are cleared when a new test is executed.
 */
class OasStubTestService(private val properties: OasStubTestProperties,
                         private val apiRegistrationService: ApiRegistrationService,
                         private val apiObserver: ApiObserver,
                         private val apiRecorder: ApiRecorder,
                         private val objectMapper: ObjectMapper) {
    fun setup() {
        clear()
        properties.definitions.forEach { (k, v) ->
            apiRegistrationService.saveApiDefinitions(k, v.toApiDefinitions()).subscribe()
        }
    }

    fun clear() {
        apiRegistrationService.getAllNames().map { name ->
            apiRegistrationService.deleteApiDefinitions(name)
        }.subscribe()
        clearTestApiMetrics()
        clearAllTestApiRecords()
    }

    /**
     * Creates an OAS API definitions programmatically
     *
     * [script] must be a Resource provided by Spring framework.
     * For example, to create a definition from the resource on classpath
     * can be written like this
     *
     * ```kotlin
     * aasStubTestService.createTestApiContext("petstore", ClassPathResource("/schema/petstore.yaml"))
     * ```
     *
     * @param name Name of the API definition. e.g. petstore
     * @param script OAS API definition resource.
     * @return OasStubTestApiContext
     */
    fun createTestApiContext(name: String, script: Resource) = OasStubTestApiContext(apiRegistrationService, name, ApiDefinitions(script.inputStream.reader().readText()))

    /**
     * Retrieves an [OasStubTestApiContext] or create an empty one
     *
     * This method also returns ones defined in external configuration file.
     *
     * @param name Name of the API definition. e.g. petstore
     * @return OasStubTestApiContext
     */
    fun getTestApiContext(name: String): OasStubTestApiContext = apiRegistrationService.getApiDefinitions(name)
        .map { def -> OasStubTestApiContext(apiRegistrationService, name, def) }
        .block()
        ?: OasStubTestApiContext(apiRegistrationService, name, ApiDefinitions())

    /**
     * Retrieves an [OasStubTestApiMetricsAggregator]
     */
    fun getTestApiMetrics(name: String): OasStubTestApiMetricsAggregator = apiObserver.getApiMetrics(name)
        .map { metrics -> OasStubTestApiMetricsAggregator(metrics.metrics.flatMap { (_, v) -> v }) }
        .orElseGet { OasStubTestApiMetricsAggregator(listOf()) }

    /**
     * Clears all the API metrics
     */
    fun clearTestApiMetrics() = apiObserver.clearApiMetrics()

    fun getTestApiRecords(name: String): OasStubTestApiRecordAggregator = OasStubTestApiRecordAggregator(
        apiRecorder.getApiRecords(name).map { it.records.map { record -> StubRecord(record, objectMapper) } }
            .orElseGet { listOf() }
    )

    fun clearTestApiRecords(name: String) = apiRecorder.clearApiRecords(name)

    fun clearAllTestApiRecords() = apiRecorder.clearAllApiRecords()
}

/**
 * Oas Stub Test Api context.
 *
 * It's a thin wrapper of [ApiDefinitions] with API name
 */
class OasStubTestApiContext
internal constructor(private val apiRegistrationService: ApiRegistrationService,
                     private val name: String,
                     // Visible for testing
                     internal val apiDefinitions: ApiDefinitions) {
    /**
     * Saves the current [ApiDefinitions] with [name]
     */
    fun save() {
        apiRegistrationService.saveApiDefinitions(name, apiDefinitions).subscribe()
    }

    /**
     * Get [ApiConfiguration] associated to [path]
     */
    fun getApiConfiguration(path: String) = Optional.ofNullable(apiDefinitions.configurations?.get(path))

    /**
     * Update specification
     */
    fun updateSpecification(resource: Resource) =
        OasStubTestApiContext(apiRegistrationService, name, apiDefinitions.updateSpecification(resource.inputStream.reader().readText()))

    /**
     * Inserts or updates the [configuration] with [path]
     */
    fun updateApi(path: String, configuration: ApiConfiguration) =
        OasStubTestApiContext(apiRegistrationService, name, apiDefinitions.updateConfiguration(path, configuration))

    /**
     * Updates header configuration of the API definition
     */
    fun updateHeaders(headers: ApiHeaders) = OasStubTestApiContext(apiRegistrationService, name, apiDefinitions.updateHeaders(headers))

    /**
     * Updates data configuration of the API definition
     */
    fun updateData(data: Map<String, Any>) = OasStubTestApiContext(apiRegistrationService, name, apiDefinitions.updateData(ApiData(data.toMutableMap())))

    /**
     * Updates delay configuration of the API definition
     */
    fun updateDelay(delay: ApiDelay?) = OasStubTestApiContext(apiRegistrationService, name, apiDefinitions.updateDelay(delay))

    /**
     * Updates options configuration of the API definition
     */
    fun updateOptions(options: ApiOptions) = OasStubTestApiContext(apiRegistrationService, name, apiDefinitions.updateOptions(options))

}

/**
 * Stub metrics aggregator.
 *
 * Example of byPath
 * ```kotlin
 * oasStubTestService.getTestApiMetrics("petstore")
 *   .byPath("/pets")
 *   .count()
 * // returns "/pets" API invocation count
 * ```
 *
 * Example of byStatus
 * ```kotlin
 * oasStubTestService.getTestApiMetrics("petstore")
 *   .byStatus(200)
 *   .count()
 * // returns API invocation count that returned HTTP status of 200
 * ```
 */
data class OasStubTestApiMetricsAggregator(private val metrics: List<ApiMetric>) {
    /**
     * Get current metrics
     */
    fun get() = metrics

    /**
     * Filters the metrics
     */
    fun filter(predicate: Predicate<ApiMetric>) = OasStubTestApiMetricsAggregator(metrics.filter { v -> predicate.test(v) })

    /**
     * Filter metrics by HTTP method of [method]
     */
    fun byMethod(method: String) = filter { m -> m.httpMethod == method }

    /**
     * Filter metrics by [path]
     */
    fun byPath(path: String) = filter { m -> m.apiPath == path }

    /**
     * Filter metrics by [template]
     */
    fun byUriTemplate(template: UriTemplate) = filter { m -> template.matches(m.apiPath) }

    /**
     * Filter metrics by [status]
     */
    fun byStatus(status: Int) = filter { m -> m.httpStatus == status }

    /**
     * Returns number of API metrics
     */
    fun count(): Int = metrics.size
}

private fun ObjectMapper.safeReadTree(value: ByteArray): JsonNode? = try {
    this.readTree(value)
} catch (e: Exception) {
    null
}

internal interface BaseStubRecord {
    /**
     * Converts body to JSON if possible
     */
    fun json(): Optional<JsonNode>
    val rawBody: Optional<ByteArray>
    val headers: Map<String, List<String>>
    fun isJson(): Boolean = json().isPresent
}

data class StubRequestRecord(private val request: ApiRequestRecord,
                             private val objectMapper: ObjectMapper): BaseStubRecord {
    private val cacheJson: Optional<JsonNode> by lazy {
        request.body.map { objectMapper.safeReadTree(it) }
    }
    override fun json(): Optional<JsonNode> = cacheJson

    override val rawBody = request.body
    override val headers = request.headers
}
data class StubResponseRecord(private val response: ApiResponseRecord,
                              private val objectMapper: ObjectMapper): BaseStubRecord {
    private val cacheJson: Optional<JsonNode> by lazy {
        response.body.map { objectMapper.safeReadTree(it) }
    }
    override fun json(): Optional<JsonNode> = cacheJson

    override val rawBody = response.body
    override val headers = response.headers
}

/**
 * Stub invocation record
 */
data class StubRecord(private val record: ApiRecord,
                      private val objectMapper: ObjectMapper) {
    private val rawRequest: ApiRequestRecord = record.request
    internal val rawResponse: ApiResponseRecord = record.response

    /**
     * Returns request of this invocation
     */
    val request: StubRequestRecord by lazy { StubRequestRecord(rawRequest, objectMapper) }

    /**
     * Returns response of this invocation
     */
    val response: StubResponseRecord by lazy { StubResponseRecord(rawResponse, objectMapper) }

    /**
     * HTTP method of this invocation
     */
    val method: String = record.method

    /**
     * API path of this invocation
     */
    val path: String = record.path
}

/**
 * Stub records aggregator.
 *
 * This aggregator is useful when you want to check what the request or response to/from the
 * OAS stub server.
 */
data class OasStubTestApiRecordAggregator(private val records: List<StubRecord>) {
    /**
     * Get current records
     */
    fun get() = records

    /**
     * Get the number of current records
     */
    fun count() = records.size

    /**
     * Filter the records by given [predicate]
     */
    fun filter(predicate: Predicate<StubRecord>) = OasStubTestApiRecordAggregator(records.filter { predicate.test(it) })

    /**
     * Filters the records by HTTP method of [method]
     */
    fun byMethod(method: String) = filter { record -> record.method == method }

    /**
     * Filters the records by API path of [path]
     */
    fun byPath(path: String) = filter { record -> record.path == path }

    /**
     * Filters the records by API path of matching with URI template of [template]
     */
    fun byUriTemplate(template: UriTemplate) = filter { record -> template.matches(record.path) }

    /**
     * Filters the records if the [name] request header exists
     */
    fun byRequestHeader(name: String) = byRequestHeader(name) { it != null }

    /**
     * Filters the records if the [name] request header contains [value]
     *
     * NOTE: HTTP can have multiple of the same headers, this filter returns if one of the values
     * is [value]
     */
    fun byRequestHeader(name: String, value: String) = byRequestHeader(name) { it?.contains(value) ?: false }

    /**
     * Filters the records if the [name] request header satisfies [predicate]
     */
    fun byRequestHeader(name: String, predicate: Predicate<List<String>?>) = byHeader(name, StubRecord::request, predicate)

    /**
     * Filters the records if the request contains body
     */
    fun byRequestBody() = byRequestBody { it != null }

    /**
     * Filters the records if the request body is [value]
     */
    fun byRequestBody(value: ByteArray) = byRequestBody { value.contentEquals(it) }

    /**
     * Filters the records if the request body satisfies [predicate]
     */
    fun byRequestBody(predicate: Predicate<ByteArray?>) = byStubBody(StubRecord::request, predicate)

    /**
     * Filters the records if the request body is JSON and contains JSON pointer [pointer] field
     */
    fun byRequestJson(pointer: String) = byRequestJson(JsonPointer.compile(pointer))

    /**
     * Filters the records if the request body is JSON and contains JSON pointer [pointer] field
     */
    fun byRequestJson(pointer: JsonPointer) = byRequestJson(pointer) { it != null }

    /**
     * Filters the records if the request body is JSON and JSON pointer [pointer] has the value of [value]
     */
    fun byRequestJson(pointer: String, value: JsonNode) = byRequestJson(JsonPointer.compile(pointer), value)

    /**
     * Filters the records if the request body is JSON and JSON pointer [pointer] has the value of [value]
     */
    fun byRequestJson(pointer: JsonPointer, value: JsonNode) = byRequestJson(pointer) { it == value }

    /**
     * Filters the records if the request body is JSON and the value of JSON pointer [pointer] satisfies [predicate]
     */
    fun byRequestJson(pointer: String, predicate: Predicate<JsonNode?>) = byRequestJson(JsonPointer.compile(pointer), predicate)

    /**
     * Filters the records if the request body is JSON and the value of JSON pointer [pointer] satisfies [predicate]
     */
    fun byRequestJson(pointer: JsonPointer, predicate: Predicate<JsonNode?>) = byJsonBody(pointer, StubRecord::request, predicate)

    /**
     * Filters the records if the response status is [status]
     */
    fun byStatus(status: Int) = filter { record -> record.rawResponse.status == status }

    /**
     * Filters the records if the [name] response header exists
     */
    fun byResponseHeader(name: String) = byResponseHeader(name) { it != null }

    /**
     * Filters the records if the [name] response header contains [value]
     *
     * NOTE: HTTP can have multiple of the same headers, this filter returns if one of the values
     * is [value]
     */
    fun byResponseHeader(name: String, value: String) = byResponseHeader(name) { it?.contains(value) ?: false }

    /**
     * Filters the records if the [name] response header satisfies [predicate]
     */
    fun byResponseHeader(name: String, predicate: Predicate<List<String>?>) = byHeader(name, StubRecord::response, predicate)

    /**
     * Filters the records if the response contains body
     */
    fun byResponseBody() = byResponseBody { it != null }

    /**
     * Filters the records if the response body is [value]
     */
    fun byResponseBody(value: ByteArray) = byResponseBody { value.contentEquals(it) }

    /**
     * Filters the records if the response body satisfies [predicate]
     */
    fun byResponseBody(predicate: Predicate<ByteArray?>) = byStubBody(StubRecord::response, predicate)

    /**
     * Filters the records if the response body is JSON and contains JSON pointer [pointer] field
     */
    fun byResponseJson(pointer: String) = byResponseJson(JsonPointer.compile(pointer))

    /**
     * Filters the records if the response body is JSON and contains JSON pointer [pointer] field
     */
    fun byResponseJson(pointer: JsonPointer) = byResponseJson(pointer) { it != null }

    /**
     * Filters the records if the response body is JSON and JSON pointer [pointer] has the value of [value]
     */
    fun byResponseJson(pointer: String, value: JsonNode) = byResponseJson(JsonPointer.compile(pointer), value)

    /**
     * Filters the records if the response body is JSON and JSON pointer [pointer] has the value of [value]
     */
    fun byResponseJson(pointer: JsonPointer, value: JsonNode) = byResponseJson(pointer) { it == value }

    /**
     * Filters the records if the response body is JSON and the value of JSON pointer [pointer] satisfies [predicate]
     */
    fun byResponseJson(pointer: String, predicate: Predicate<JsonNode?>) = byResponseJson(JsonPointer.compile(pointer), predicate)

    /**
     * Filters the records if the response body is JSON and the value of JSON pointer [pointer] satisfies [predicate]
     */
    fun byResponseJson(pointer: JsonPointer, predicate: Predicate<JsonNode?>) = byJsonBody(pointer, StubRecord::response, predicate)


    private fun byStubBody(getter: (StubRecord) -> BaseStubRecord, predicate: Predicate<ByteArray?>) = filter { record -> predicate.test(getter(record).rawBody.getOrNull()) }
    private fun byHeader(name: String, getter: (StubRecord) -> BaseStubRecord, predicate: Predicate<List<String>?>) =  filter { record -> predicate.test(getter(record).headers[name]) }
    private fun byJsonBody(pointer: JsonPointer, getter: (StubRecord) -> BaseStubRecord, predicate: Predicate<JsonNode?>) = filter { record -> getter(record).json().filter { predicate.test(it.at(pointer)) }.isPresent }
}