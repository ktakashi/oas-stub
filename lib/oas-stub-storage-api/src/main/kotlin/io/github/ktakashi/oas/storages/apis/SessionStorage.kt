package io.github.ktakashi.oas.storages.apis

import io.github.ktakashi.oas.model.ApiMetric
import io.github.ktakashi.oas.model.ApiMetrics
import io.github.ktakashi.oas.plugin.apis.Storage
import java.util.Optional

private const val API_METRICS_KEY = "*io.github.ktakashi.oas.api.metric*"
interface SessionStorage: Storage {
    // internal use
    fun addApiMetric(name: String, path: String, metric: ApiMetric) {
        val metrics = getApiMetrics(name).orElseGet {
            ApiMetrics()
        }.addApiMetric(path, metric)
        val holder = get(API_METRICS_KEY, DefaultApiMetricsHolder::class.java).orElseGet { DefaultApiMetricsHolder() }
        holder.metrics[name] = metrics
        put(API_METRICS_KEY, holder)
    }

    fun getApiMetrics(name: String): Optional<ApiMetrics> = get(API_METRICS_KEY, DefaultApiMetricsHolder::class.java)
        .map { holder -> holder.metrics[name] }

    fun clearApiMetrics() {
        delete(API_METRICS_KEY)
    }
}

private data class DefaultApiMetricsHolder(val metrics: MutableMap<String, ApiMetrics> = mutableMapOf())
