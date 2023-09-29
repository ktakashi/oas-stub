package io.github.ktakashi.oas.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.Duration
import java.time.OffsetDateTime

data class ApiMetric(
    val requestTimestamp: OffsetDateTime,
    @JsonFormat(shape = JsonFormat.Shape.STRING) val executionTime: Duration,
    val apiPath: String,
    val httpMethod: String,
    val httpStatus: Int,
    val exception: Throwable?
)

data class ApiMetrics(val metrics: MutableMap<String, MutableList<ApiMetric>> = mutableMapOf()) {
    fun addApiMetric(path: String, metric: ApiMetric): ApiMetrics {
        metrics.compute(path) { _, v ->
            v?.apply { add(metric) } ?: mutableListOf(metric)
        }
        return this
    }
}