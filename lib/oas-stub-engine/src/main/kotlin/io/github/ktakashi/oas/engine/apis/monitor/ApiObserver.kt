package io.github.ktakashi.oas.engine.apis.monitor

import io.github.ktakashi.oas.model.ApiMetric
import io.github.ktakashi.oas.storages.apis.SessionStorage

class ApiObserver(private val sessionStorage: SessionStorage) {

    fun addApiMetric(name: String, path: String, metric: ApiMetric) = sessionStorage.addApiMetric(name, path, metric)

    fun getApiMetrics(name: String) = sessionStorage.getApiMetrics(name)

    fun clearApiMetrics() = sessionStorage.clearApiMetrics()
}