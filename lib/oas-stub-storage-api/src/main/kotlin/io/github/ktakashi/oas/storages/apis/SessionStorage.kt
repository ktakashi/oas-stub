package io.github.ktakashi.oas.storages.apis

import io.github.ktakashi.oas.api.http.RequestContext
import io.github.ktakashi.oas.api.http.ResponseContext
import io.github.ktakashi.oas.model.ApiMetric
import io.github.ktakashi.oas.model.ApiMetrics
import io.github.ktakashi.oas.api.storage.Storage
import io.github.ktakashi.oas.model.ApiRecord
import io.github.ktakashi.oas.model.ApiRecords
import io.github.ktakashi.oas.model.ApiRequestRecord
import io.github.ktakashi.oas.model.ApiResponseRecord
import java.util.Optional
import java.util.TreeMap

private const val API_METRICS_KEY = "*io.github.ktakashi.oas.api.metrics*"
private const val API_RECORDS_KEY = "*io.github.ktakashi.oas.api.records*"

interface SessionStorage: Storage {
    // internal use
    fun addApiMetric(name: String, path: String, metric: ApiMetric) {
        val metrics = getApiMetrics(name).orElseGet { ApiMetrics() }.addApiMetric(path, metric)
        val holder = get(API_METRICS_KEY, DefaultApiMetricsHolder::class.java).orElseGet { DefaultApiMetricsHolder() }
        holder.metrics[name] = metrics
        put(API_METRICS_KEY, holder)
    }

    fun getApiMetrics(name: String): Optional<ApiMetrics> = get(API_METRICS_KEY, DefaultApiMetricsHolder::class.java)
        .map { holder -> holder.metrics[name] }

    fun clearApiMetrics() {
        delete(API_METRICS_KEY)
    }

    fun addApiRecord(name: String, request: RequestContext, response: ResponseContext) {
        val key = getApiRecordKey(name)
        val record = ApiRecord(request.method, request.apiPath, request.toRecord(), response.toRecord())
        val records = getApiRecords(name).orElseGet { ApiRecords() }.addApiRecord(record)
        put(key, records)
    }

    fun getApiRecords(name: String) = get(getApiRecordKey(name), ApiRecords::class.java)

    fun clearApiRecords(name: String) {
        delete(getApiRecordKey(name))
    }
}

private fun getApiRecordKey(name: String) = "$API_RECORDS_KEY:$name"
private fun RequestContext.toRecord() = ApiRequestRecord(contentType, ensureTreeMap(headers, contentType), cookies.map { (k, v) -> k to v.toString() }.toMap(), content)
private fun ResponseContext.toRecord() = ApiResponseRecord(status, contentType, ensureTreeMap(headers, contentType), content)

private fun ensureTreeMap(headers: Map<String, List<String>>, contentType: Optional<String>): Map<String, List<String>> = (when (headers) {
    is TreeMap -> headers
    else -> TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER).apply { putAll(headers) }
}).also {
    if (contentType.isPresent && !it.containsKey("Content-Type")) {
        it["Content-Type"] = listOf(contentType.get())
    }
}
private data class DefaultApiMetricsHolder(val metrics: MutableMap<String, ApiMetrics> = mutableMapOf())
