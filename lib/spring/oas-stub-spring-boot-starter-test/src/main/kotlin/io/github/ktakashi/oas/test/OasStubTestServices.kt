package io.github.ktakashi.oas.test

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
import java.util.Optional
import java.util.function.Predicate
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
                         private val apiRecorder: ApiRecorder) {
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

    fun getTestApiRecords(name: String): List<ApiRecord> = apiRecorder.getApiRecords(name).map<List<ApiRecord>> { it.records }.orElseGet { listOf() }

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
                     private val apiDefinitions: ApiDefinitions) {
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
    fun updateData(data: Map<String, Any>) = OasStubTestApiContext(apiRegistrationService, name, apiDefinitions.updateData(ApiData(data)))

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
